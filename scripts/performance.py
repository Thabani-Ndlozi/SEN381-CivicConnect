#!/usr/bin/env python3
"""Small read-heavy performance exercise; requires live staging and real results."""
import argparse, base64, concurrent.futures, datetime, json, os, pathlib, statistics, time, urllib.request, urllib.error
p=argparse.ArgumentParser();p.add_argument('--base-url',default='http://localhost:8082');p.add_argument('--requests',type=int,default=40);p.add_argument('--workers',type=int,default=4);p.add_argument('--env-file',default='.env.staging');p.add_argument('--report',default='evidence/performance-results.json')
a=p.parse_args()
if pathlib.Path(a.env_file).exists():
 for line in pathlib.Path(a.env_file).read_text().splitlines():
  if line and not line.startswith('#') and '=' in line:
   k,v=line.split('=',1);os.environ.setdefault(k,v)
u,pw=os.getenv('STAGING_MANAGER_USERNAME'),os.getenv('STAGING_MANAGER_PASSWORD')
if not u or not pw:raise SystemExit('Missing staging manager credentials')
headers={'Authorization':'Basic '+base64.b64encode(f'{u}:{pw}'.encode()).decode()}
def attempt(_):
 start=time.perf_counter()
 try:
  with urllib.request.urlopen(urllib.request.Request(a.base_url+'/api/management/summary',headers=headers),timeout=15) as res:
   res.read();return {'ms':round((time.perf_counter()-start)*1000,2),'ok':res.status==200,'status':res.status}
 except urllib.error.HTTPError as e:return {'ms':round((time.perf_counter()-start)*1000,2),'ok':False,'status':e.code}
 except Exception as e:return {'ms':round((time.perf_counter()-start)*1000,2),'ok':False,'status':type(e).__name__}
start=time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:records=list(pool.map(attempt,range(a.requests)))
elapsed=time.perf_counter()-start;times=sorted(r['ms'] for r in records);n=len(times)
report={'observed_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'endpoint':'GET /api/management/summary',
        'base_url':a.base_url,'requests':n,'workers':a.workers,'elapsed_seconds':round(elapsed,3),'ok':sum(r['ok'] for r in records),
        'errors':n-sum(r['ok'] for r in records),'p50_ms':times[int((n-1)*.50)] if n else None,
        'p95_ms':times[int((n-1)*.95)] if n else None,'throughput_req_s':round(n/elapsed,2) if elapsed else None,
        'team_proposed_local_smoke_target':'zero errors, p95 < 1000 ms for 40 requests, 4 workers; not approved SLO',
        'results':records}
out=pathlib.Path(a.report);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='results'},indent=2))
print(f'Wrote {out}')
raise SystemExit(0 if report['errors']==0 else 1)
