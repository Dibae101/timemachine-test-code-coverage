# Calculator module

Calculator tool. Main feature. 

## Graph

```mermaid
%%{
  init: {
    'theme': 'dark'
  }
}%%

graph LR
  subgraph :core
    :core:designsystem["designsystem"]
    :core:common["common"]
    :core:model["model"]
    :core:ui["ui"]
    :core:navigation["navigation"]
    :core:evaluatto["evaluatto"]
    :core:datastore["datastore"]
    :core:data["data"]
  end
  subgraph :feature
    :feature:calculator["calculator"]
    :feature:datecalculator["datecalculator"]
  end
  :sharedApp --> :feature:calculator
  :sharedApp --> :feature:datecalculator
  :feature:calculator --> :core:designsystem
  :feature:calculator --> :core:common
  :feature:calculator --> :core:model
  :feature:calculator --> :core:ui
  :feature:calculator --> :core:navigation
  :feature:calculator --> :core:evaluatto
  :feature:calculator --> :core:datastore
  :feature:calculator --> :core:data
  :feature:datecalculator --> :core:common
  :feature:datecalculator --> :core:ui
  :feature:datecalculator --> :core:navigation
  :feature:datecalculator --> :core:designsystem
  :feature:datecalculator --> :core:datastore
  :feature:datecalculator --> :core:data
  :feature:datecalculator --> :core:model

classDef focus fill:#769566,stroke:#fff,stroke-width:2px,color:#fff;
class :feature:calculator focus
class :feature:datecalculator focus
```