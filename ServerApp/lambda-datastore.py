import json
import os
import datetime 
import random 
import urlparse
# external dependencies
import pymysql
# private dependencies
import db_config


"""
https://www.scribd.com/presentation/2569355/Geo-Distance-Search-with-MySQL
https://www.scribd.com/document/24744656/Distance-Calculation-Between-2-Points-on-Earth


Simplified approximate query - assume near euclidan geometry. 
111.3278 = 1 degree in km
57.29 = 180/PI conversion degrees to radians

SELECT longitude, latitude , sqrt(
		POW( 111.3278 * ( latitude - 31.124 ) , 2 ) + POW( 111.3278 * ( 34.6 - longitude ) * COS( latitude / 57.29 ) , 2 )
		) AS distance_sq
	FROM events 
	HAVING distance_sq <10
	ORDER BY distance_sq
"""

# Main function 
def lambda_handler(event, context):
	# write event to database, and then fetch the current results
	assert event["context"]["http-method"] in ["POST","GET"] , "Method %r is not supported" % event["context"]["http-method"]
	# return event
	print event["context"]["http-method"]
	connection=connect_database()
	if event["context"]["http-method"]=="POST" and event["params"]["header"]["Content-type"] == "application/json":
		response = write_event(event, connection)
		longitude=event["body-json"]["longitude"]
		latitude=event["body-json"]["latitude"]
	elif event["context"]["http-method"]=="POST" and event["params"]["header"]["Content-type"] == "application/x-www-form-urlencoded":
		body=dict(urlparse.parse_qsl(event["body-json"]))
		return body
		longitude=event["body-json"]["longitude"]
		latitude=event["body-json"]["latitude"]
	elif event["context"]["http-method"]== "GET": 
		longitude=event["params"]["querystring"]["longitude"]
		latitude=event["params"]["querystring"]["latitude"]
	else:
		return {}
	response = read_stats(connection, longitude, latitude) 
	return response


def connect_database():
	server_address = os.getenv('DATABASE', db_config.db_host)
	# Connect to database 
	connection = pymysql.connect(	server_address, 
							user=db_config.db_username, 
							passwd=db_config.db_password, 
							db=db_config.db_name, 
							connect_timeout=25, 
							autocommit=True)
	return connection
#-------------------------------------------------------------------------------------------
def write_event( event, connection):
	deviceid=event["body-json"]["device"]["deviceid"]
	eventdate=event["body-json"]["timestamp"]
	userid=event["body-json"]["userid"]
	longitude=event["body-json"]["longitude"]
	latitude=event["body-json"]["latitude"]
	measurements=event["body-json"]["measurements"]
	for point in measurements: 
		substanceid=point["substance"]
		measurement=point["value"]
		with connection.cursor() as cur:
			statement= """INSERT INTO events(eventdate, deviceid, userid, 
						longitude, latitude, substanceid, measurement) VALUES (%s,%s,%s,%s,%s,%s,%s) """
			result = cur.execute( 
				statement, 
				(eventdate, deviceid, userid, longitude, latitude, substanceid, measurement ) 
				)	
	return	result	

#-------------------------------------------------------------------------------------------
def read_stats( connection, longitude, latitude):
	statement = """
		select longitude, latitude, distance_sq, substances.code , measurement 
		from 
			(SELECT longitude as longitude, latitude , sqrt(
				POW( 111.3278 * ( latitude - %r ) , 2 ) + POW( 111.3278 * ( 34.6 - %r ) * COS( latitude / 57.29 ) , 2 )
				) AS distance_sq, 
				substanceid , avg(measurement) as measurement 
			FROM events 
			group by longitude, latitude, distance_sq, events.substanceid 
			HAVING distance_sq <%r ) as events 
		JOIN substances ON events.substanceid = substances.id
		WHERE events.measurement > substances.threshold
		
		ORDER BY distance_sq;
		""" % (latitude,longitude, 10) 
	with connection.cursor(pymysql.cursors.DictCursor) as cur:
		result = cur.execute( statement )	
		result = cur.fetchall()
		result_wip={}	
	for item in result:
		longitude=float(item["longitude"])
		latitude=float(item["latitude"])
		tuple=(longitude, latitude)
		print tuple
		if not bool(result_wip.get(tuple)):  
			result_wip.update( { tuple : {"longitude": longitude, "latitude": latitude, "data":[]}} ) 
		result_wip[tuple]["data"].append( 
							{"code":item["code"], "measurement": float(item["measurement"])}
							)
	final_list = [ value for key, value in result_wip.iteritems()]
	return	final_list				
	
#-------------------------------------------------------------------------------------------


if __name__=="__main__":
	timestamp=datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")
	rnd = random.randrange(123, 170)
	print timestamp, rnd
	# quit()
	POST_json =	{
			  "body-json": 	{
					   "version": 0.1,
					   "device": {
						"arduinoversion": 8.2,
						"make": "samsung",
						"model": "note 3",
						"os": "android",
						"osversion": "5.0.3",
						"deviceid": "IMI1234QA876987809"
					  },
					  "userid": "7890",
					  "longitude": 34.50178922,
					  "latitude": 31.12312311,
					  "timestamp": timestamp,
					  "measurements": [
						{ "substance": "1", "value": 123 },
						{ "substance": "2", "value": 321 },
						{ "substance": "3", "value": 456 }
					  ]
					},
			  "params": {
				"path": {},
				"querystring": {},
				"header": {
				  "Via": "1.1 5fc330730b7a22af558c1164ae769565.cloudfront.net (CloudFront)",
				  "CloudFront-Is-Desktop-Viewer": "true",
				  "CloudFront-Is-SmartTV-Viewer": "false",
				  "CloudFront-Forwarded-Proto": "https",
				  "X-Forwarded-For": "46.120.227.175, 216.137.60.73",
				  "CloudFront-Viewer-Country": "IL",
				  "Content-type": "application/json",
				  "Accept": "*/*",
				  "User-Agent": "curl/7.43.0",
				  "X-Amzn-Trace-Id": "Root=1-58a8d59a-5444ea3142a0f36500efdc28",
				  "Host": "61cn8cug1h.execute-api.eu-west-1.amazonaws.com",
				  "X-Forwarded-Proto": "https",
				  "X-Amz-Cf-Id": "i-KX32dw8q5T3IQUPe6YupJDzmlrRyjzdUt_jvZdtXkaVBAFAPiA7g==",
				  "CloudFront-Is-Tablet-Viewer": "false",
				  "X-Forwarded-Port": "443",
				  "CloudFront-Is-Mobile-Viewer": "false"
				}
			  },
			  "stage-variables": {},
			  "context": {
				"cognito-authentication-type": "",
				"http-method": "POST",
				"account-id": "",
				"resource-path": "/homeapp/measurements",
				"authorizer-principal-id": "",
				"user-arn": "",
				"request-id": "2889eec0-f630-11e6-b221-af714872239e",
				"source-ip": "46.120.227.175",
				"caller": "",
				"api-key": "",
				"user-agent": "curl/7.43.0",
				"user": "",
				"cognito-identity-pool-id": "",
				"api-id": "61cn8cug1h",
				"resource-id": "mrfymt",
				"stage": "Prod",
				"cognito-identity-id": "",
				"cognito-authentication-provider": ""
			  }
			}

	POST_url_encoded=	{
			  "body-json": 	"userid=7890&longitude=34.50178922&latitude=31.12312311&substance=1&value=123",
			  "params": {
				"path": {},
				"querystring": {},
				"header": {
				  "Via": "1.1 5fc330730b7a22af558c1164ae769565.cloudfront.net (CloudFront)",
				  "CloudFront-Is-Desktop-Viewer": "true",
				  "CloudFront-Is-SmartTV-Viewer": "false",
				  "CloudFront-Forwarded-Proto": "https",
				  "X-Forwarded-For": "46.120.227.175, 216.137.60.73",
				  "CloudFront-Viewer-Country": "IL",
				  "Content-type": "application/x-www-form-urlencoded",
				  "Accept": "*/*",
				  "User-Agent": "curl/7.43.0",
				  "X-Amzn-Trace-Id": "Root=1-58a8d59a-5444ea3142a0f36500efdc28",
				  "Host": "61cn8cug1h.execute-api.eu-west-1.amazonaws.com",
				  "X-Forwarded-Proto": "https",
				  "X-Amz-Cf-Id": "i-KX32dw8q5T3IQUPe6YupJDzmlrRyjzdUt_jvZdtXkaVBAFAPiA7g==",
				  "CloudFront-Is-Tablet-Viewer": "false",
				  "X-Forwarded-Port": "443",
				  "CloudFront-Is-Mobile-Viewer": "false"
				}
			  },
			  "stage-variables": {},
			  "context": {
				"cognito-authentication-type": "",
				"http-method": "POST",
				"account-id": "",
				"resource-path": "/homeapp/measurements",
				"authorizer-principal-id": "",
				"user-arn": "",
				"request-id": "2889eec0-f630-11e6-b221-af714872239e",
				"source-ip": "46.120.227.175",
				"caller": "",
				"api-key": "",
				"user-agent": "curl/7.43.0",
				"user": "",
				"cognito-identity-pool-id": "",
				"api-id": "61cn8cug1h",
				"resource-id": "mrfymt",
				"stage": "Prod",
				"cognito-identity-id": "",
				"cognito-authentication-provider": ""
			  }
			}
	
	event=POST_url_encoded		
	context={}
	result=lambda_handler(event, context)
	print json.dumps(result)
	
	
"""
This code expects the following input in the event object: 


"""