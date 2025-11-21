const fs = require('fs');
const { parse } = require('yaml');

module.exports = async ({ github, context, core }) => {
  // Extract component name from label
  const labelName = context.payload.label.name;

  if (!labelName.startsWith('component:')) {
    core.setFailed('Label does not match expected pattern');
    return;
  }

  const componentName = labelName.replace('component:', '');
  console.log(`Processing component: ${componentName}`);

  // Read and parse component_owners.yml
  const yamlContent = fs.readFileSync('.github/component_owners.yml', 'utf8');
  const data = parse(yamlContent);

  if (!data || !data.components) {
    core.setFailed('Invalid component_owners.yml structure');
    return;
  }

  const components = data.components;

  if (!(componentName in components)) {
    core.setFailed(`Component '${componentName}' not found in component_owners.yml`);
    return;
  }

  const owners = components[componentName];

  if (!owners || owners.length === 0) {
    core.setFailed(`No owners found for component '${componentName}'`);
    return;
  }

  console.log(`Found owners: ${owners.join(', ')}`);

  // Assign the issue to the owners
  const issueNumber = context.payload.issue.number;

  await github.rest.issues.addAssignees({
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: issueNumber,
    assignees: owners
  });

  console.log(`Successfully assigned issue #${issueNumber} to ${owners.join(', ')}`);
};
