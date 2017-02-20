CREATE TABLE IF NOT EXISTS substances (
  id int(10) NOT NULL AUTO_INCREMENT,
  code varchar(10)  NOT NULL, 
  description varchar(256) NOT NULL , 
  threshold dec(20,5) NOT NULL , 
  PRIMARY KEY (id)
  ) ;

CREATE TABLE IF NOT EXISTS devices (
  id int(10) NOT NULL AUTO_INCREMENT,
  deviceid varchar(256) NOT NULL , 
  userid int(10) NOT NULL,
  appversion varchar(32) NOT NULL,
  arduinoversion varchar(32) NOT NULL,
  make varchar(32) NOT NULL,
  model varchar(32) NOT NULL,
  os varchar(32) NOT NULL,
  osversion varchar(32) NOT NULL,
  registrationdate timestamp, 
  PRIMARY KEY (id)
  ) ;

CREATE TABLE IF NOT EXISTS events (
  id int(10) NOT NULL AUTO_INCREMENT,
  eventdate timestamp , 
  deviceid varchar(256) NOT NULL , 
  userid int(10) NOT NULL, 
  longitude dec(11,9) NOT NULL,
  latitude dec(11,9) NOT NULL,
  substanceid int(10) not null, 
  measurement dec(18,9) not null, 
  PRIMARY KEY (id)
  ) ;
  

				
insert into substances (code, description, threshold) values('co','carbon monoxide', 123);
insert into substances (code, description, threshold) values('co2','carbon dioxide', 456);
insert into substances (code, description, threshold) values('nox','nitrous oxide', 678);


insert into devices (deviceid,userid,appversion,arduinoversion,make,model,os,osversion,registrationdate) 
			values('SHALOMSUNG1234567890',1,'0.1a','0.2','Samsung','Note 3 9006B','android','5.0.2','2017-02-15 12:35:00');


			

