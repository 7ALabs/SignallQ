$assignments = @(
  @{ num = 3;  agent = "Camilo"; skills = "linka-arch" }
  @{ num = 4;  agent = "Camilo"; skills = "linka-arch" }
  @{ num = 5;  agent = "Camilo"; skills = "linka-arch" }
  @{ num = 6;  agent = "Camilo"; skills = "linka-arch" }
  @{ num = 7;  agent = "Camilo"; skills = "linka-arch" }
  @{ num = 19; agent = "Camilo"; skills = "linka-arch" }
  @{ num = 20; agent = "Camilo"; skills = "linka-arch" }
  @{ num = 21; agent = "Camilo"; skills = "linka-arch" }
  @{ num = 22; agent = "Camilo"; skills = "linka-arch" }
  @{ num = 10; agent = "Claudete"; skills = "linka-design,linka-arch" }
  @{ num = 11; agent = "Claudete"; skills = "linka-design,linka-arch" }
  @{ num = 12; agent = "Claudete"; skills = "linka-design,linka-arch" }
  @{ num = 23; agent = "Claudete"; skills = "linka-design,linka-arch" }
  @{ num = 17; agent = "Claudete"; skills = "linka-design,linka-arch" }
  @{ num = 14; agent = "Gema"; skills = "linka-docs,linka-arch" }
  @{ num = 1;  agent = "Gema"; skills = "linka-docs,linka-arch" }
  @{ num = 8;  agent = "Gema"; skills = "linka-docs,linka-arch" }
  @{ num = 13; agent = "Gema"; skills = "linka-docs,linka-arch" }
  @{ num = 15; agent = "Gema"; skills = "linka-docs,linka-arch" }
  @{ num = 2;  agent = "Rodrigo"; skills = "linka-arch,linka-docs" }
  @{ num = 1;  agent = "Rodrigo"; skills = "linka-arch,linka-docs" }
  @{ num = 8;  agent = "Rodrigo"; skills = "linka-arch,linka-docs" }
  @{ num = 13; agent = "Rodrigo"; skills = "linka-arch,linka-docs" }
  @{ num = 21; agent = "Rodrigo"; skills = "linka-arch,linka-docs" }
  @{ num = 16; agent = "Marina"; skills = "linka-arch" }
  @{ num = 5;  agent = "Marina"; skills = "linka-arch" }
  @{ num = 9;  agent = "Marina"; skills = "linka-arch" }
  @{ num = 20; agent = "Marina"; skills = "linka-arch" }
  @{ num = 18; agent = "Bras"; skills = "linka-arch,linka-design" }
  @{ num = 17; agent = "Bras"; skills = "linka-arch,linka-design" }
  @{ num = 19; agent = "Bras"; skills = "linka-arch,linka-design" }
  @{ num = 24; agent = "Bras"; skills = "linka-arch,linka-design" }
  @{ num = 9;  agent = "Bras"; skills = "linka-arch,linka-design" }
  @{ num = 23; agent = "Bras"; skills = "linka-arch,linka-design" }
)

$env:Path += ";C:\Program Files\GitHub CLI"
$repo = "7ALabs/linka-android"

$issueAgents = @{}
foreach ($assignment in $assignments) {
  $num = $assignment.num
  if (-not $issueAgents.ContainsKey($num)) {
    $issueAgents[$num] = @()
  }
  $issueAgents[$num] += @{ agent = $assignment.agent; skills = $assignment.skills }
}

foreach ($issueNum in ($issueAgents.Keys | Sort-Object)) {
  $agents = $issueAgents[$issueNum]

  if ($agents.Count -eq 1) {
    $agentLine = "Agent: $($agents[0].agent)"
    $skillLine = "Skills: $($agents[0].skills)"
  } else {
    $agentNames = $agents | ForEach-Object { $_.agent } | Join-String -Separator ", "
    $agentLine = "Collaborative Agents: $agentNames"
    $skillAll = $agents | ForEach-Object { $_.skills } | Join-String -Separator " | "
    $skillLine = "Skills: $skillAll"
  }

  $body = "## Agent Assignment`n`n$agentLine`n$skillLine`n`nRefer to .claude/plans/AGENTS.md for temperament, specialty, and recommended sequence."

  Write-Host "Updating issue #$issueNum with $($agents.Count) agent(s)..."
  & gh issue comment $issueNum --repo $repo --body "$body"
  Start-Sleep -Milliseconds 300
}

Write-Host "All 24 issues updated with humanized agent assignments!"
