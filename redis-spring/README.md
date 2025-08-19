Application: city-api.jar

To Run: java -jar city-api.jar

on port: 3030

localhost:3030/open-city-api/<zip-code>

zip-code: 00601, 00602 ... 99929

Sample Response:
{
    "zip": "10001",
    "lat": 40.75065,
    "lng": -73.99718,
    "city": "New York",
    "stateId": "NY",
    "stateName": "New York",
    "population": 24117,
    "density": 15153.7,
    "temperature": 99
}


------------------------------------
WebSocket Chat: 

WebSocket Test Client: Chrome Extension "WebSocket Test Client"

ws://localhost:8080/chat?room=room1&user=john

Send Chat:

{
"type": "CHAT_MESSAGE",
"room": "room1",
"user": "john",
"message": "12"
}


Load History:

{
"type": "LOAD_HISTORY",
"room": "room1",
"page": "1",
"size": "10"
}

--------------------------------
Geo:

API: http://localhost:8080/geo/75224?radius=5&unit=MILES