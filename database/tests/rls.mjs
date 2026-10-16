import {PGlite} from '@electric-sql/pglite';
import fs from 'node:fs';import {fileURLToPath} from 'node:url';import assert from 'node:assert/strict';
const db=new PGlite();const root=fileURLToPath(new URL('../../', import.meta.url));
for(const name of fs.readdirSync(root+'backend/src/main/resources/db/migration').sort())await db.exec(fs.readFileSync(root+'backend/src/main/resources/db/migration/'+name,'utf8'));
await db.exec("CREATE ROLE runtime NOSUPERUSER NOBYPASSRLS; GRANT USAGE ON SCHEMA public TO runtime; GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA public TO runtime;");
const ids=['11111111-1111-1111-1111-111111111111','22222222-2222-2222-2222-222222222222'];
for(let i=0;i<2;i++){await db.query("INSERT INTO service_requests VALUES ($1,$2,'Test','Test request','OTHER','SUBMITTED',null,now(),now(),0,'Location','NORMAL')",[ids[i],`r${i}@example.com`]);await db.query("INSERT INTO request_audit VALUES(gen_random_uuid(),$1,$2,'REQUEST_CREATED',null,'SUBMITTED','Created',now())",[ids[i],`r${i}@example.com`]);}
await db.exec('SET ROLE runtime');let checks=0;
async function identity(email,role){await db.query("SELECT set_config('app.email',$1,false),set_config('app.role',$2,false)",[email,role])}
async function count(table,n){assert.equal((await db.query(`SELECT * FROM ${table}`)).rows.length,n);checks++}
async function denied(sql){await assert.rejects(db.exec(sql));checks++}
await identity('r0@example.com','REQUESTER');await count('service_requests',1);await count('request_audit',1);
assert.equal((await db.query("UPDATE service_requests SET title='Hacked' RETURNING id")).rows.length,0);checks++;
await denied("INSERT INTO app_users(email,full_name,password_hash,role) VALUES('bad@example.com','Bad','hash','MANAGER')");
await denied("INSERT INTO app_users(email,full_name,password_hash,role) VALUES('staff@example.com','Staff','hash','STAFF')");
await identity('staff@example.com','STAFF');await count('service_requests',2);await count('request_audit',2);
await db.query("UPDATE service_requests SET owner_id='staff@example.com',status='ACCEPTED' WHERE id=$1",[ids[0]]);
await identity('other@example.com','STAFF');assert.equal((await db.query("UPDATE service_requests SET status='RESOLVED' WHERE id=$1 RETURNING id",[ids[0]])).rows.length,0);checks++;
await identity('manager@example.com','MANAGER');await db.exec("INSERT INTO app_users(email,full_name,password_hash,role) VALUES('staff@example.com','Staff','hash','STAFF')");checks++;
await db.query("UPDATE service_requests SET status='RESOLVED' WHERE id=$1",[ids[0]]);checks++;
await denied("INSERT INTO app_users(email,full_name,password_hash,role) VALUES('anothermanager@example.com','Manager','hash','MANAGER')");
await identity('','');await count('service_requests',0);await count('request_audit',0);
await db.exec("INSERT INTO app_users(email,full_name,password_hash,role) VALUES('new@example.com','Requester','hash','REQUESTER')");checks++;
console.log(`PASS: all four migrations; ${checks} RLS isolation and account-policy assertions.`);await db.close();
