#!/bin/sh
docker run --rm -it -e DATABASE_URL="jdbc:mysql://db:3306/blue_harvest_dev?user=summit&password=qw23er" --add-host=db:172.17.0.1 -p 3009:3000 --name rampart rampart
