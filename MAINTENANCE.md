# Component maintenance policy

This policy defines component ownership responsibilities and what happens when a component lacks enough active owners. The goal is to tie continued component inclusion to active maintenance, aligning incentives and protecting the long-term health of the repository.

## Adding a component

Before submitting a new component, open an issue to obtain maintainer agreement on:

* Fit with this repository's scope.
* At least two owners who agree to the responsibilities below and have the expertise to maintain the component.

## Component ownership

Every component must have at least two active owners, listed in [.github/component_owners.yml](.github/component_owners.yml) and its README. Owners must be members of the OpenTelemetry GitHub organization so that review and issue assignment automation can reach them.

Owners must:

* Maintain all aspects of the component, including issue triage and pull request review.
* Respond to explicit GitHub mentions requesting action within 14 calendar days.

Responsiveness, not commit frequency, determines whether an owner is active. Responses must move pull requests toward merge or closure and issues toward an actionable decision, including identifying blockers or declining work. Owners need not implement every request or resolve it within 14 days.

[Repository maintainers](README.md#maintainers) manage shared infrastructure, admission, and enforcement of this policy. They do not automatically become owners of components that lack volunteers.

## Removing an owner

Owners have 14 calendar days to respond to explicit GitHub mentions requesting action on a component issue or pull request. Only requests from repository approvers or the author of the issue or pull request count toward removal. Unanswered requests enter the rolling count when their response deadline expires. Count each separate task once. Reminders, repeated tags, or multiple comments about the same task do not count as additional requests. Late responses do not remove requests from the count.

Maintainers remove unresponsive owners with three or more unanswered requests within any rolling three-month period, or four or more within any rolling six-month period.

## Recruiting replacement owners

When a component falls below two active owners, maintainers open a public recruitment issue and start a 60-calendar-day recruitment period. They update the project and component READMEs with the ownership shortfall, a link to the issue, and the applicable deadline:

* **Unstable components:** The scheduled removal date, at the end of the recruitment period.
* **Stable components:** The scheduled deprecation date, at the end of the recruitment period, followed by removal after a release containing the deprecations.

See [Retiring a component](#retiring-a-component) for the removal requirements.

If the component returns to at least two active owners before removal, maintainers cancel retirement, close the recruitment issue, and remove the notices and any deprecations added for retirement. Partial recruitment does not restart the deadline.

Maintainers may volunteer as component owners under the same responsibilities.

Continued usage, including production usage, does not exempt a component from this requirement or extend the deadline. Users who need continued maintenance are encouraged to volunteer or arrange maintenance outside this repository.

## Retiring a component

If the recruitment period expires without restoring two active owners:

* **Unstable components:** Remove the component from source and future releases.
* **Stable components:** Mark the component as deprecated in its README and public API Javadoc, including `@Deprecated` annotations and `@deprecated` tags where applicable. Publish at least one release containing these deprecations before removing the component from source and future releases.

Provide alternatives or migration guidance where available. Previously published artifacts remain available but are no longer maintained after retirement.

Removed components may be reintroduced through the [new-component admission process](#adding-a-component).
