# Allure result post-processor
# Injects parentSuite and epic labels, removes stale epic tags
# Args: $ps (parentSuite value), $epic (epic value)

.labels = [
  (.labels // [])[]
  | select(
      .name != "parentSuite"
      and .name != "epic"
      and (
        if .name == "tag"
        then (.value | startswith("allure.label.epic:") | not)
        else true
        end
      )
    )
  | if .name == "subSuite"
    then .value = (.value | gsub(" *@allure[.]label[.]epic:\\S*"; "") | ltrimstr(" ") | rtrimstr(" "))
    else .
    end
]
+ [
  { "name": "parentSuite", "value": $ps },
  { "name": "epic", "value": $epic }
]
