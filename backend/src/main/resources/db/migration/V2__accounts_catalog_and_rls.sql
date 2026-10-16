CREATE TABLE departments(name varchar(120) PRIMARY KEY);
INSERT INTO departments VALUES ('Public Works'),('Community Services'),('IT & Systems'),('Safety & Security');
CREATE TABLE app_users (
 email varchar(120) PRIMARY KEY CHECK(email=lower(email)),
 full_name varchar(120) NOT NULL, password_hash varchar(100) NOT NULL,
 role varchar(20) NOT NULL CHECK(role IN ('REQUESTER','STAFF','MANAGER')),
 department varchar(120) REFERENCES departments(name), enabled boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE request_categories(code varchar(40) PRIMARY KEY,label varchar(100) NOT NULL);
INSERT INTO request_categories VALUES ('FACILITY_FAULT','Facility Fault'),('DAMAGED_EQUIPMENT','Damaged Equipment'),
 ('SECURITY_CONCERN','Security Concern'),('IT_SUPPORT','IT Support'),('MAINTENANCE','Maintenance'),('LOST_PROPERTY','Lost Property'),('OTHER','Other');
CREATE TABLE request_statuses(code varchar(30) PRIMARY KEY,label varchar(100) NOT NULL,sort_order int NOT NULL);
INSERT INTO request_statuses VALUES ('SUBMITTED','Open',1),('ACCEPTED','Accepted',2),('IN_PROGRESS','In Progress',3),('RESOLVED','Resolved',4),('CLOSED','Closed',5),('REJECTED','Rejected',6);
CREATE TABLE help_articles(id int PRIMARY KEY,title text NOT NULL,body text NOT NULL);
INSERT INTO help_articles VALUES (1,'How It Works','Submit a request, track its progress, and view updates from your service team.'),
 (2,'Be specific','Include what the issue is, where it is located, and when you noticed it.'),
 (3,'Choose the right category','This helps your service team review your request.');
ALTER TABLE service_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_requests FORCE ROW LEVEL SECURITY;
CREATE POLICY request_read ON service_requests FOR SELECT USING (
 current_setting('app.role',true) IN ('STAFF','MANAGER') OR requester_id=current_setting('app.email',true));
CREATE POLICY request_create ON service_requests FOR INSERT WITH CHECK (
 current_setting('app.role',true)='REQUESTER' AND requester_id=current_setting('app.email',true) AND status='SUBMITTED' AND owner_id IS NULL);
CREATE POLICY request_update ON service_requests FOR UPDATE USING (
 current_setting('app.role',true)='MANAGER' OR (current_setting('app.role',true)='STAFF' AND (owner_id=current_setting('app.email',true) OR (owner_id IS NULL AND status='SUBMITTED'))))
 WITH CHECK (current_setting('app.role',true)='MANAGER' OR (current_setting('app.role',true)='STAFF' AND (owner_id=current_setting('app.email',true) OR (owner_id IS NULL AND status='REJECTED'))));
ALTER TABLE request_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE request_audit FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_read ON request_audit FOR SELECT USING (EXISTS(SELECT 1 FROM service_requests r WHERE r.id=request_id));
CREATE POLICY audit_create ON request_audit FOR INSERT WITH CHECK (actor=current_setting('app.email',true) AND EXISTS(SELECT 1 FROM service_requests r WHERE r.id=request_id));
-- No delete policy: requests and audit evidence cannot be deleted by the application.
