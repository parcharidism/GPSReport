This tiny basic program converts GPS data into HTML reports for better viewing and understanding. The reports contain starting date, starting point, ending date, ending point and how long the object stayed there before leaving for its next trip.
It is basically made specifically for the TKSTAR website export of GPS locations.

**Acknowledgements**
- Currently a trip is considered any stop that lasts more than 10 minutes. I am planning to change that in the future with a command line argument.
- The program is respecting the 1-second limit interval of Nominatim (OpenStreetMaps) for resolving addresses therefore it will take its time especially if you have many points.


**Inputs:**
The program is expecting a CSV file with just 4 columns in this particular order:
- An incremental number
- The time in dd/mm/yyyy hh:mm:ss format 
- Latitude
- Longitude

The sources file will be put into a seperate folder that the program will create.

**Basic Instructions for TKSTAR export files**
Most likely, the exported file will contain 3 header rows. We delete the first two leaving only the true headers of the data. After that we need to format the Lon and Lat cells to contain only numbers without dots (.) the dots will be added by the program during processing
Save that file as a CSV (UTF-8) using **;** as the separator

This is under development therefore it might not work in all cases
