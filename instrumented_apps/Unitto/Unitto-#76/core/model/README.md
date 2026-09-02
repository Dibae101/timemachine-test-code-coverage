# Model module

Common classes and interfaces that need to be shared between modules (didn't fit in `:core:data` module)

## Graph

```mermaid
%%{
  init: {
    'theme': 'dark'
  }
}%%

graph LR
  subgraph :core
    :core:model["model"]
    :core:datastore["datastore"]
    :core:common["common"]
    :core:data["data"]
  end
  subgraph :feature
    :feature:settings["settings"]
    :feature:calculator["calculator"]
    :feature:converter["converter"]
    :feature:timezone["timezone"]
    :feature:datecalculator["datecalculator"]
    :feature:glance["glance"]
  end
  :feature:settings --> :core:model
  :core:datastore --> :core:model
  :core:model --> :core:common
  :feature:calculator --> :core:model
  :feature:converter --> :core:model
  :feature:timezone --> :core:model
  :feature:datecalculator --> :core:model
  :core:data --> :core:model
  :feature:glance --> :core:model

classDef focus fill:#769566,stroke:#fff,stroke-width:2px,color:#fff;
class :core:model focus
```