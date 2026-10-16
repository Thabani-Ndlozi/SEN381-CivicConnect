ALTER TABLE service_requests ADD COLUMN location varchar(500), ADD COLUMN priority varchar(20) CHECK(priority IN ('LOW','NORMAL','HIGH','URGENT'));
CREATE TABLE priorities(code varchar(20) PRIMARY KEY,label text NOT NULL,sort_order int NOT NULL);
INSERT INTO priorities VALUES ('LOW','Low',1),('NORMAL','Normal',2),('HIGH','High',3),('URGENT','Urgent',4);
