#!/usr/bin/env python3
"""Live black-box API/E2E suite. NEVER fabricates execution results."""
import argparse, base64, json, os, pathlib, sys, unittest, urllib.error, urllib.request, uuid, time

PARSER = argparse.ArgumentParser()
PARSER.add_argument('--base-url', default='http://localhost:8082')
PARSER.add_argument('--env-file', default='.env.staging')
PARSER.add_argument('--report', default='evidence/smoke-results.json')
OPTIONS, _ = PARSER.parse_known_args()

def load_secrets(path):
    file = pathlib.Path(path)
    if file.exists():
        for row in file.read_text().splitlines():
            if row and not row.startswith('#') and '=' in row:
                key,value=row.split('=', 1)
                os.environ.setdefault(key, value.strip().strip('"'))

load_secrets(OPTIONS.env_file)
SECRETS = {role: (os.getenv(f'STAGING_{role}_USERNAME'),os.getenv(f'STAGING_{role}_PASSWORD'))
           for role in ('REQUESTER','STAFF','MANAGER')}

def api(method, path, role='REQUESTER', payload=None, unauthenticated=False):
    headers={'Content-Type':'application/json'}
    if not unauthenticated:
        username,password=SECRETS[role]
        if not username or not password: raise RuntimeError(f'Missing {role} staging credentials')
        headers['Authorization']='Basic '+base64.b64encode(f'{username}:{password}'.encode()).decode()
    data=None if payload is None else json.dumps(payload).encode()
    request=urllib.request.Request(OPTIONS.base_url+path,method=method,headers=headers,data=data)
    try:
        with urllib.request.urlopen(request,timeout=12) as response:
            content=response.read(); return response.status, json.loads(content) if content else None
    except urllib.error.HTTPError as error:
        content=error.read()
        try: body=json.loads(content)
        except (ValueError,UnicodeDecodeError): body=content.decode(errors='replace')
        return error.code,body

def new_request():
    title=f'M3 smoke {uuid.uuid4().hex[:10]}'
    code,record=api('POST','/api/requests',payload={'title':title,'description':'Automated test - safely disposable','category':'FACILITY_FAULT'})
    if code!=201: raise AssertionError((code,record))
    return record

class LiveTests(unittest.TestCase):
    """At least 3 API integration and 2 distinct E2E journeys; runs against real staging stack."""
    def test_API01_missing_authentication_denied(self):
        status,_=api('GET','/api/requests/mine',unauthenticated=True)
        self.assertEqual(401,status)

    def test_API02_requester_cannot_read_staff_queue(self):
        status,_=api('GET','/api/staff/requests',role='REQUESTER')
        self.assertEqual(403,status)

    def test_API03_validation_negative_equivalence_partition(self):
        status,_=api('POST','/api/requests',payload={'title':' ','description':'Missing usable title','category':'OTHER'})
        self.assertEqual(400,status)

    def test_API04_invalid_state_transition_conflict(self):
        request=new_request();id=request['id']
        status,accepted=api('POST',f'/api/staff/requests/{id}/accept','STAFF');self.assertEqual(200,status)
        status,_=api('POST',f'/api/staff/requests/{id}/status','STAFF',{'targetStatus':'CLOSED','comment':'Invalid leap','version':accepted['version']})
        self.assertEqual(409,status)
        status,detail=api('GET',f'/api/staff/requests/{id}','STAFF')
        self.assertEqual('ACCEPTED',detail['status'])

    def test_API05_non_owner_cannot_change_request(self):
        request=new_request();id=request['id']
        status,accepted=api('POST',f'/api/staff/requests/{id}/accept','STAFF');self.assertEqual(200,status)
        status,_=api('POST',f'/api/staff/requests/{id}/status','MANAGER',{'targetStatus':'IN_PROGRESS','comment':'Wrong owner','version':accepted['version']})
        self.assertEqual(403,status)

    def test_API06_role_specific_history_access(self):
        request=new_request();id=request['id']
        status,events=api('GET',f'/api/requests/{id}/history','REQUESTER')
        self.assertEqual(200,status);self.assertTrue(events)
        self.assertIsNone(events[0].get('actor'))
        status,_=api('GET',f'/api/management/requests/{id}/history','REQUESTER')
        self.assertEqual(403,status)

    def test_E2E01_requester_submission_to_completion(self):
        request=new_request();id=request['id']
        status,mine=api('GET','/api/requests/mine','REQUESTER')
        self.assertEqual(200,status);self.assertIn(id,[entry['id'] for entry in mine])
        status,accepted=api('POST',f'/api/staff/requests/{id}/accept','STAFF')
        self.assertEqual(200,status);self.assertEqual('ACCEPTED',accepted['status'])
        record=accepted  # seed version for the first iteration
        for target in ('IN_PROGRESS','RESOLVED','CLOSED'):
            status,record=api('POST',f'/api/staff/requests/{id}/status','STAFF',
                              {'targetStatus':target,'comment':f'Automated validation {target}','version':record['version']})
            self.assertEqual(200,status);self.assertEqual(target,record['status'])
        status,events=api('GET',f'/api/requests/{id}/history','REQUESTER')
        self.assertEqual(200,status)
        self.assertEqual(['SUBMITTED','ACCEPTED','IN_PROGRESS','RESOLVED','CLOSED'],
                         [e['toStatus'] for e in events])
        self.assertTrue(all(e.get('actor') is None for e in events))

    def test_E2E02_manager_can_trace_controlled_staff_work(self):
        request=new_request();id=request['id']
        status,record=api('POST',f'/api/staff/requests/{id}/accept','STAFF')
        self.assertEqual(200,status)
        status,changed=api('POST',f'/api/staff/requests/{id}/status','STAFF',
                     {'targetStatus':'IN_PROGRESS','comment':'Work began','version':record['version']})
        self.assertEqual(200,status)
        status,audit=api('GET',f'/api/management/requests/{id}/history','MANAGER')
        self.assertEqual(200,status)
        self.assertEqual(3,len(audit))
        self.assertEqual(SECRETS['STAFF'][0],audit[-1]['actor'])
        status,summary=api('GET','/api/management/summary','MANAGER')
        self.assertEqual(200,status);self.assertGreaterEqual(summary['IN_PROGRESS'],1)
        status,by_category=api('GET','/api/management/breakdown','MANAGER')
        self.assertEqual(200,status);self.assertGreaterEqual(by_category['FACILITY_FAULT']['IN_PROGRESS'],1)

class JsonRecorder(unittest.TextTestResult):
    def startTest(self, test):
        super().startTest(test)
        self.test_start=time.perf_counter()
    def stopTest(self, test):
        super().stopTest(test)
        records.append({'test_id':test._testMethodName,'seconds':round(time.perf_counter()-self.test_start,3),
                        'status':'FAIL' if any(t is test for t,_ in self.failures+self.errors) else 'PASS',
                        'level':'E2E' if '_E2E' in test._testMethodName else 'API/integration'})

if __name__=='__main__':
    records=[]
    runner=unittest.TextTestRunner(verbosity=2,resultclass=JsonRecorder)
    suite=unittest.defaultTestLoader.loadTestsFromTestCase(LiveTests)
    result=runner.run(suite)
    report={'environment':OPTIONS.base_url,'suite':'LiveTests','executed_at_utc':__import__('datetime').datetime.now(__import__('datetime').timezone.utc).isoformat(),
            'total':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'cases':records}
    output=pathlib.Path(OPTIONS.report);output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,indent=2)+'\n')
    print(f'Wrote {output}')
    sys.exit(0 if result.wasSuccessful() else 1)
