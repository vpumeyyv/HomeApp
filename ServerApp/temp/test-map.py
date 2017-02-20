import json
"""
    [ 
        {"longitude": 42.21, "latitude": 34.56,
         "data":[
                    { "metric": "co1", "value": 45 }
                ]
        },
        {"longitude": 12.01, "latitude": 25.123456,
         "data":[
                    { "metric": "co1", "value": 45 },
                    { "metric": "co2", "value": 47 }, 
                    { "metric": "co3", "value": 12.1 }
                ]
        }
    ]
[{"longitude":42.21,"latitude":34.56,"data":[{"metric":"co1","value":45}]},{"longitude":12.01,"latitude":25.123456,"data":[{"metric":"co1","value":45},{"metric":"co2","value":47},{"metric":"co3","value":12.1}]}]
"""

mylist = [ 
		{"longitude": 42.21, "latitude": 34.56, "metric": "co1", "value": 45 }, 
		{"longitude": 12.01, "latitude": 25.123456, "metric": "co1", "value": 45 },
		{"longitude": 12.01, "latitude": 25.123456, "metric": "co2", "value": 47 },
		{"longitude": 12.01, "latitude": 25.123456, "metric": "co3", "value": 12.1 }
		]

otherlist={}	
for item in mylist:
	tuple=(item["longitude"], item["latitude"])
	if not bool(otherlist.get(tuple)):  
		otherlist.update( { tuple : {"longitude": item["longitude"], "latitude": item["latitude"], "data":[]}} ) 
	otherlist[tuple]["data"].append( 
						{"metric":item["metric"], "value":item["value"]}
						)
final_list = [ value for key, value in otherlist.iteritems()]
print final_list
latitude=123.456
longitude=456.789

statement = """
	select longitude, latitude, distance_sq, substances.code , measurement from 
	(SELECT longitude, latitude , sqrt(
		POW( 111.3278 * ( latitude - %r ) , 2 ) + POW( 111.3278 * ( 34.6 - %r ) * COS( latitude / 57.29 ) , 2 )
		) AS distance_sq, 
		substanceid , avg(measurement) as measurement 
	FROM events 
	group by longitude, latitude, distance_sq, events.substanceid 
	HAVING distance_sq <%r) as events 
	JOIN substances ON events.substanceid = substances.id
	WHERE events.measurement > substances.threshold
	
	ORDER BY distance_sq;
	""" % (latitude,longitude, 100)
print statement

"""
{
	(42.21, 34.56): {
						"latitude": 34.56, 
						"data": [
								{"metric": "co1", "value": 45}
								], 
						"longitude": 42.21}, 
	(12.01, 25.123456): {
						"latitude": 25.123456, 
						"data": [
								{"metric": "co1", "value": 45}, 
								{"metric": "co2", "value": 47}, 
								{"metric": "co3", "value": 12.1}
								], 
						"longitude": 12.01}
}
"""


