[![buy_me_a_coffee_badge](https://img.shields.io/badge/buy%20me%20a%20coffee-donate-yellow.svg)](https://www.buymeacoffee.com/lucianourgal)
# MasterThesis_code
 This code was developed between the end of 2016 until July 2018 using Java 1.8.<br>
 It was used for my master thesis in UTFPR. Its mainly about OpenStreetMap map processing, Map matching using taxi trajectory data and origin destination matriz estimation (ODME) solutions using multiple algorithms.
 
 # Optimization algorithms code / other ODME solutions
 1 - Genetic Algorithm (GA)<br>
 2 - Particle Swarm Optimization (PSO)<br>
 3 - Least Squares (LS)<br>
 4 - GUSEK MILP code generation<br>
 
 # Some of the features
 1 - Uses jfreechart library to generate multiple graphics<br>
 2 - Transforms OpenStreetMap .osm into a .dat for quicker use later<br>
 3 - Reads Taxi Service Trajectory - Prediction Challenge, ECML PKDD 2015 Data Set and maps its GPS points to the OSM network (map matching)<br>
 4 - Uses multiples optimization algorithms to solve the Origin Destination Matriz Estimation (ODME) problem<br>
 4 - resultsAlgs class summarizes the quality of all algorithms by using different metrics (R2, GEH, MAE, RMSE etc) and saves to a .csv and .dat file for durability<br>
 5 - Uses statistical test to analyze if the algorithms results have significant statistical differences (summarized in resultsAlgs.csv<br>
 6 - Is able to make the ODME in two different cases: For a Curitiba and a Porto network (check the master thesis pdf for more details)<br>
<br>

PS. Some files had to be zipped in order to share this directory.
