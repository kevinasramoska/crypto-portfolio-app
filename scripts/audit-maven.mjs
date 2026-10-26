import { readFile } from "node:fs/promises";

// Generate with Maven dependency:tree -Dscope=runtime -DoutputType=json -DoutputFile=target/dependencies.json.
const tree = JSON.parse(await readFile(process.argv[2] ?? "backend/target/dependencies.json", "utf8"));
const coordinates = new Map();
function visit(dependency, root = false) {
  if (!root && dependency.groupId && dependency.artifactId && dependency.version) {
    const name = `${dependency.groupId}:${dependency.artifactId}`;
    coordinates.set(`${name}@${dependency.version}`, { package: { ecosystem: "Maven", name }, version: dependency.version });
  }
  for (const child of dependency.children ?? []) visit(child);
}
visit(tree, true);
if (!coordinates.size) throw new Error("Empty dependency inventory; refusing to report a clean scan");
const queries = [...coordinates.values()];
let findings = 0;
for (let offset = 0; offset < queries.length; offset += 100) {
  const batch = queries.slice(offset, offset + 100);
  const response = await fetch("https://api.osv.dev/v1/querybatch", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ queries: batch }), signal: AbortSignal.timeout(45000),
  });
  if (!response.ok) throw new Error(`Advisory service failed: ${response.status}`);
  const body = await response.json();
  if (!Array.isArray(body.results) || body.results.length !== batch.length) throw new Error("Incomplete advisory response");
  body.results.forEach((result, index) => {
    if (result.vulns?.length) {
      findings += result.vulns.length;
      console.error(`${batch[index].package.name}@${batch[index].version}: ${result.vulns.map(v => v.id).join(", ")}`);
    }
  });
}
console.log(`Scanned ${queries.length} resolved runtime packages; ${findings} advisory matches.`);
if (findings) process.exitCode = 1;
