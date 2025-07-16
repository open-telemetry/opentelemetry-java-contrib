# Produced Metrics


## Metric `ibm.mq.message.retry.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.message.retry.count` | Gauge | `{messages}` | Number of message retries | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.message.retry.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.status`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.status` | Gauge | `1` | Channel status | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.status` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.max.sharing.conversations`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.max.sharing.conversations` | Gauge | `{conversations}` | Maximum number of conversations permitted on this channel instance. | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.max.sharing.conversations` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.current.sharing.conversations`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.current.sharing.conversations` | Gauge | `{conversations}` | Current number of conversations permitted on this channel instance. | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.current.sharing.conversations` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.byte.received`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.byte.received` | Gauge | `{bytes}` | Number of bytes received | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.byte.received` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.byte.sent`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.byte.sent` | Gauge | `{bytes}` | Number of bytes sent | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.byte.sent` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.buffers.received`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.buffers.received` | Gauge | `{buffers}` | Buffers received | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.buffers.received` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.buffers.sent`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.buffers.sent` | Gauge | `{buffers}` | Buffers sent | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.buffers.sent` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.message.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.message.count` | Gauge | `{messages}` | Message count | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.message.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.start.time` | int | The start time of the channel as seconds since Epoch. | `1748462702` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `job.name` | string | The job name | `0000074900000003` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.open.input.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.open.input.count` | Gauge | `{applications}` | Count of applications sending messages to the queue | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.open.input.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.open.output.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.open.output.count` | Gauge | `{applications}` | Count of applications consuming messages from the queue | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.open.output.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.high.queue.depth`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.high.queue.depth` | Gauge | `{percent}` | The current high queue depth | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.high.queue.depth` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.service.interval`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.service.interval` | Gauge | `{percent}` | The queue service interval | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.service.interval` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.queue.depth.full.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.queue.depth.full.event` | Counter | `{events}` | The number of full queue events | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.queue.depth.full.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.queue.depth.high.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.queue.depth.high.event` | Counter | `{events}` | The number of high queue events | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.queue.depth.high.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.queue.depth.low.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.queue.depth.low.event` | Counter | `{events}` | The number of low queue events | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.queue.depth.low.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.uncommitted.messages`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.uncommitted.messages` | Gauge | `{messages}` | Number of uncommitted messages | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.uncommitted.messages` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.oldest.msg.age`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.oldest.msg.age` | Gauge | `microseconds` | Queue message oldest age | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.oldest.msg.age` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.current.max.queue.filesize`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.current.max.queue.filesize` | Gauge | `mib` | Current maximum queue file size | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.current.max.queue.filesize` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.current.queue.filesize`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.current.queue.filesize` | Gauge | `mib` | Current queue file size | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.current.queue.filesize` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.instances.per.client`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.instances.per.client` | Gauge | `{instances}` | Instances per client | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.instances.per.client` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.message.deq.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.message.deq.count` | Gauge | `{messages}` | Message dequeue count | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.message.deq.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.message.enq.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.message.enq.count` | Gauge | `{messages}` | Message enqueue count | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.message.enq.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.queue.depth`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.queue.depth` | Gauge | `{messages}` | Current queue depth | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.queue.depth` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.service.interval.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.service.interval.event` | Gauge | `1` | Queue service interval event | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.service.interval.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.reusable.log.size`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.reusable.log.size` | Gauge | `mib` | The amount of space occupied, in megabytes, by log extents available to be reused. | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.reusable.log.size` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.manager.active.channels`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.manager.active.channels` | Gauge | `{channels}` | The queue manager active maximum channels limit | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.manager.active.channels` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.restart.log.size`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.restart.log.size` | Gauge | `mib` | Size of the log data required for restart recovery in megabytes. | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.restart.log.size` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.max.queue.depth`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.max.queue.depth` | Gauge | `{messages}` | Maximum queue depth | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.max.queue.depth` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.onqtime.1`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.onqtime.1` | Gauge | `microseconds` | Amount of time, in microseconds, that a message spent on the queue, over a short period | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.onqtime.1` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.onqtime.2`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.onqtime.2` | Gauge | `microseconds` | Amount of time, in microseconds, that a message spent on the queue, over a longer period | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.onqtime.2` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.name` | string | The queue name | `DEV.DEAD.LETTER.QUEUE` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.type` | string | The queue type | `local-normal` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.message.received.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.message.received.count` | Gauge | `{messages}` | Number of messages received | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.message.received.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.message.sent.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.message.sent.count` | Gauge | `{messages}` | Number of messages sent | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.message.sent.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.max.instances`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.max.instances` | Gauge | `{instances}` | Max channel instances | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.max.instances` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `channel.name` | string | The name of the channel | `DEV.ADMIN.SVRCONN` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `channel.type` | string | The type of the channel | `server-connection`; `cluster-receiver`; `amqp` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.connection.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.connection.count` | Gauge | `{connections}` | Active connections count | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.connection.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.manager.status`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.manager.status` | Gauge | `1` | Queue manager status | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.manager.status` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.heartbeat`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.heartbeat` | Gauge | `1` | Queue manager heartbeat | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.heartbeat` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.archive.log.size`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.archive.log.size` | Gauge | `mib` | Queue manager archive log size | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.archive.log.size` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.manager.max.active.channels`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.manager.max.active.channels` | Gauge | `{channels}` | Queue manager max active channels | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.manager.max.active.channels` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.manager.statistics.interval`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.manager.statistics.interval` | Gauge | `1` | Queue manager statistics interval | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.manager.statistics.interval` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.publish.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.publish.count` | Gauge | `{publications}` | Topic publication count | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.publish.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `topic.name` | string | The name of the topic | `dev/` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.subscription.count`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.subscription.count` | Gauge | `{subscriptions}` | Topic subscription count | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.subscription.count` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `topic.name` | string | The name of the topic | `dev/` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.listener.status`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.listener.status` | Gauge | `1` | Listener status | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.listener.status` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `listener.name` | string | The listener name | `listener` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.unauthorized.event`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.unauthorized.event` | Counter | `{events}` | Number of authentication error events | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.unauthorized.event` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `application.name` | string | The application name | `Wordle`; `JMSService` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
| `user.name` | string | The user name | `foo`; `root` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |



## Metric `ibm.mq.manager.max.handles`

| Name     | Instrument Type | Unit (UCUM) | Description    | Stability |
| -------- | --------------- | ----------- | -------------- | --------- |
| `ibm.mq.manager.max.handles` | Gauge | `{events}` | Max open handles | ![Development](https://img.shields.io/badge/-development-blue) |


### `ibm.mq.manager.max.handles` Attributes

| Attribute  | Type | Description  | Examples  | [Requirement Level](https://opentelemetry.io/docs/specs/semconv/general/attribute-requirement-level/) | Stability |
|---|---|---|---|---|---|
| `queue.manager` | string | The name of the queue manager | `MQ1` | `Required` | ![Development](https://img.shields.io/badge/-development-blue) |
