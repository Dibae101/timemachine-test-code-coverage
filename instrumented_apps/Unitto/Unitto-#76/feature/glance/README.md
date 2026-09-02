# Glance module

Android home screen widgets. Do NOT merge with other modules. Keep separate for multiplatform.

## Graph

```mermaid
%%{
  init: {
    'theme': 'dark'
  }
}%%

graph LR
  subgraph :core
    :core:common["common"]
    :core:data["data"]
    :core:database["database"]
    :core:datastore["datastore"]
    :core:designsystem["designsystem"]
    :core:evaluatto["evaluatto"]
    :core:model["model"]
    :core:navigation["navigation"]
    :core:themmo["themmo"]
    :core:ui["ui"]
  end
  subgraph :feature
    :feature:glance["glance"]
  end
  :sharedApp --> :feature:glance
  :feature:glance --> :core:common
  :feature:glance --> :core:data
  :feature:glance --> :core:database
  :feature:glance --> :core:datastore
  :feature:glance --> :core:designsystem
  :feature:glance --> :core:evaluatto
  :feature:glance --> :core:model
  :feature:glance --> :core:navigation
  :feature:glance --> :core:themmo
  :feature:glance --> :core:ui

classDef focus fill:#769566,stroke:#fff,stroke-width:2px,color:#fff;
class :feature:glance focus
```