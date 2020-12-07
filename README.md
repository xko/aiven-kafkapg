Kafka to Postgres in Aiven cloud
-------------------
This demo is a [homework](Home_Assigment_Aiven_Backend_Java.md) for job application at [Aiven](https://aiven.io/). 
It demonstrates recording system metrics to Postgres database via apache Kafka. Kafka producer and consumer are 
implemented with [Monix-kafka](https://github.com/monix/monix-kafka) library and 
[Slick](https://scala-slick.org/) is used to access Postgres. 

All the needed services are deployed to Aiven cloud by terraform-based automation.  

### Setting up

##### 0. Prerequisites and notes
- An empty project in [Aiven console](https://console.aiven.io/) with enough credits.
- all `init-*` scripts require **Python** (either 2 or 3) to be available on `$PATH` as `python`
- all `init-*` scripts require [aiven CLI](https://github.com/aiven/aiven-client) installed and available on `$PATH` 
- `init-aiven` additionally requires **[Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli)**
- main code is writen in [Scala](https://www.scala-lang.org/) 2.13, which requires **JDK 11** or later.
  It is [sbt](https://www.scala-sbt.org)-based. Minimal sbt [launcher](bin/sbt) is provided, so only JDK is required, 
  but you can of course use your own sbt installation
- all the shell commands below are to be run from the root of this repo clone 
  
##### 1. Aiven infrastructure
**WARNING**: this works on actual cloud infrastructure! Never run against production project!

Run `./init-aiven` and confirm each step. 
After this is done, further updates to [terraform config](aiven.tf) can be applied with `terraform apply` 
from the same clone of this repo. If needed to run from different copy, the file `terraform.tfstate` 
needs to be moved there.

##### 2. Kafka client config
`./init-kafka` will setup everything needed to connect to Kafka. 
This can be done independently on different copies of this repo, setting-up as many instances as desired.   

##### 3. Postgres client config
`./init-postgres` will setup everything needed to run Postgres reader. 
This can be done independently on different copies of this repo as well.   

### Testing

[Integration tests](src/it/scala/aiven/kafkapg) run against real servers in aiven cloud, deployed as described above. 
They can be started with `bin/sbt it:test`, or individually - `bin/sbt "it:testOnly ..."`  


### Running

Several main classes defined in [Mains.scala](src/main/scala/aiven/kafkapg/Mains.scala). 
The sbt command `run` will let you choose the one to run. Or use `bin/sbt "runMain ..."` to start individually 
- `aiven.kafkapg.ToKafkaConnectEvery3s` sends messages to the topic, read by Kafka-connect service. 
  In this case consumer in not needed - data end up in postgress automaticall
- `aiven.kafkapg.ToKafkaEvery3s` sends messages to the topic, read by one of the consumers below
- `aiven.kafkapg.NoiseToKafkaEvery5s`- same as above, but sends noisy messages to test consumer's error-tolerance
- `aiven.kafkapg.FromKafkaToConsole` consumes messages and prints to console, ignoring the errors
- `aiven.kafkapg.FromKafkaToPg` consumes messages and stores to Postgres, currently fails on errors ("dead-letters" behavior is still TBD)

The last two read data from postgres. Both accept host name as an argument, defaulting to all hosts. E.g. :
-  `bin/sbt "runMain  aiven.kafkapg.FromPgLast10Records storm"` will show to see last 10 records from host "storm"
-  `bin/sbt "runMain  aiven.kafkapg.FromPgAvgCPULastHour"` will show average of the CPU load across all hosts during last hour
   or None if there are no records

All the mains above can be started in parallel in different sbt instances to show the actual message flow. 
(Warning "..sbt server already running.." can be ignored.)  