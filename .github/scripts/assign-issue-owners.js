const fs = require('fs');
const { parse } = require('yaml');
const { Octokit } = require('@octokit/rest');

async function main() {
  // Get inputs from environment
  const token = process.env.GITHUB_TOKEN;
  const labelName = process.env.LABEL_NAME;
  const issueNumber = parseInt(process.env.ISSUE_NUMBER);
  const owner = process.env.REPO_OWNER;
  const repo = process.env.REPO_NAME;

  if (!labelName.startsWith('component:')) {
    console.error('Label does not match expected pattern');
    process.exit(1);
  }

  const componentName = labelName.replace('component:', '');
  console.log(`Processing component: ${componentName}`);

  // Read and parse component_owners.yml
  const yamlContent = fs.readFileSync('.github/component_owners.yml', 'utf8');
  const data = parse(yamlContent);

  if (!data || !data.components) {
    console.error('Invalid component_owners.yml structure');
    process.exit(1);
  }

  const components = data.components;

  if (!(componentName in components)) {
    console.error(`Component '${componentName}' not found in component_owners.yml`);
    process.exit(1);
  }

  const owners = components[componentName];

  if (!owners || owners.length === 0) {
    console.error(`No owners found for component '${componentName}'`);
    process.exit(1);
  }

  console.log(`Found owners: ${owners.join(', ')}`);

  // Assign the issue to the owners
  const octokit = new Octokit({ auth: token });

  await octokit.rest.issues.addAssignees({
    owner,
    repo,
    issue_number: issueNumber,
    assignees: owners
  });

  console.log(`Successfully assigned issue #${issueNumber} to ${owners.join(', ')}`);
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
