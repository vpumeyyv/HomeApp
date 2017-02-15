import json
"""
https://www.scribd.com/presentation/2569355/Geo-Distance-Search-with-MySQL
https://www.scribd.com/document/24744656/Distance-Calculation-Between-2-Points-on-Earth


Simplified approximate query - assume near euclidan geometry. 
111.3278 = 1 degree in km
57.29 = 180/PI conversion degrees to radians

SELECT latitude, longitude, (
    POW(111.3278 * (latitude - @@my_lat ), 2) +
    POW(111.3278 * (@@my_lon - longitude) * COS(latitude / 57.29), 2)) AS distance_sq
FROM TableName HAVING distance_sq < 0.1*0.1 ORDER BY distance_sq;

"""


def lambda_handler(event, context):
	response = [
				  {
					"longitude": 31,
					"latitude": 34,
					"data": [
					  {
						"gas": "co",
						"value": 45
					  }
					]
				  },
				  {
					"longitude": 31.34,
					"latitude": 34.31,
					"data": [
					  {
						"gas": "co",
						"value": 45
					  },
					  {
						"gas": "co2",
						"value": 47
					  }
					]
				  }
				]
	return response
	

if __name__=="__main__":
	event=	{
			  "version": 0.1,
			  "device": {
				"arduinoversion": 8.2,
				"make": "samsung",
				"model": "note 3",
				"os": "android",
				"osversion": "5.0.3",
				"deviceid": "IMI1234QA876987809"
			  },
			  "userid": "123",
			  "longitude": 34.5,
			  "latitude": 31.123,
			  "timestamp": "2017-02-14 20:59:33.257000",
			  "measurements": [
				{ "substance": "co2", "value": 123 },
				{ "substance": "co", "value": 321 },
				{ "substance": "nox", "value": 456 }
			  ]
			}

	context={}
	result=lambda_handler(event, context)
	print json.dumps(result)
	
	
"""
This code expects the following input in the event object: 


"""