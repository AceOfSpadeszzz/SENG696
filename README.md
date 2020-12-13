# SENG696 Multi-Agent Covid Alert System
 Project & Document for the course seng 696
 
 This project simulates the communication between agents for a agent-based covid alert system. There are 4 kinds of agents:
 
 User agent: The users of the system. They can update their condition and location.
 
 PHS agent(Public Health Service): It can review the postive cases and their address && location, and issue an alert or a safety notification to a region.
 
 Lab agnet: It can review the suspected cases and decided if it is a postive case.
 
 Alert agent: It distributes the alert issued by the PHS agent to the User agents.
 
 For running the program, you need to have mysql connector and jade installed.
 
 The command for starting the program is
 
 ```
 java jade.Boot -agents "controller:Start()"
 ```
 
 Special thanks for our TA's instruction and tutorial.
 
 [Link to our TA's tutorial](https://github.com/masoudkarimif/multiagent-number-guessing-game)
 
 

