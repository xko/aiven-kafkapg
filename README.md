Kafka to Postgres in Aiven cloud
-------------------
This demo is a [homework](Home_Assigment_Aiven_Backend_Java.md) for job application at [Aiven](https://aiven.io/). 
It contains a small application, feeding some system metrics to the Kafka topic, from where they are delivered to Postgres 
database by Kafka-Connect connector. There's also a little postgres client to make sure they have reached the destination.

All the needed services are deployed to Aiven cloud by terraform-based automation.  

### Setting up

##### 0. Prerequisites and notes
- An empty project in [Aiven console](https://console.aiven.io/) with enough credits. Trial should be enough - during 
  development of this demo, I was billed approximately $20 per day
- all `init-*` scripts require **Python** (either 2 or 3) to be available on `$PATH` as `python`
- all `init-*` scripts require [aiven CLI](https://github.com/aiven/aiven-client) installed and available on `$PATH` 
- `init-aiven` additionally requires **[Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli)**
- main code is writen in [Scala](https://www.scala-lang.org/) 2.13, which requires **JDK 11** or later.
  It is [sbt](https://www.scala-sbt.org)-based. Minimal sbt [launcher](bin/sbt) is provided, so only JDK is required, 
  but you can of course use your own sbt installation
- all the commands below are to be run from the root of this repo clone 
  
##### 1. Aiven infrastructure
**WARNING**: this works on actual cloud infrastructure! Never run against production project!

Run `./init-aiven` and confirm each step. 
After this is done, further updates to [terraform config](aiven.tf) can be applied with `terraform apply` 
from the same clone of this repo. If needed to run from different copy, the file `terraform.tfstate` 
needs to be moved there.

##### 2. Kafka client config
`./init-kafka` will setup everything needed to run Kafka producer. 
This can be done independently on different copies of this repo, setting-up as many producers as desired.   

##### 3. Postgres client config
`./init-postgres` will setup everything needed to run Postgres reader. 
This can be done independently on different copies of this repo as well.   

     
### Running
##### Kafka producer
Run `./producer` or in sbt shell: `runMain aiven.kafkapg.KafkaPublisher`

##### Postgres reader
There are couple of `App`s defined in [PgReader.scala](src/main/scala/aiven/kafkapg/PgReader.scala), hoping to illustrate
potential practical use of such solution. They use [Slick](https://scala-slick.org/doc/3.3.3/queries.html)'s Scala-native queries.
All accept host name as an argument, defaulting to all hosts. 
These can be run with sbt command `runMain`:
-  `bin/sbt "runMain  aiven.kafkapg.Last10From storm"` will show to see last 10 records from host "storm"
-  `bin/sbt "runMain  aiven.kafkapg.AvgCPULastHour"` will show average of the CPU load across all hosts during last hour
   or None if there are no records

