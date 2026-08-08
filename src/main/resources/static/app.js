async function analyzeQuery() {
    const query = document.getElementById('queryInput').value.trim();

    if (!query) {
        alert('Please enter a SQL query first.');
        return;
    }

    // Show loading
    document.getElementById('loading').style.display = 'block';
    document.getElementById('report').style.display = 'none';

    try {
        const response = await fetch('http://localhost:8080/api/analyze', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query: query })
        });

        const data = await response.json();
        displayReport(data);

    } catch (error) {
        alert('Error connecting to backend. Make sure the app is running.');
    } finally {
        document.getElementById('loading').style.display = 'none';
    }
}

function displayReport(data) {
    // Severity banner
    const banner = document.getElementById('severityBanner');
    if (data.severity === 'RED') {
        banner.className = 'red-banner';
        banner.textContent = '🔴 CRITICAL — Very Slow Query Detected';
    } else if (data.severity === 'YELLOW') {
        banner.className = 'yellow-banner';
        banner.textContent = '🟡 WARNING — Moderate Performance Issue';
    } else {
        banner.className = 'green-banner';
        banner.textContent = '🟢 GOOD — Query is Optimized';
    }

    // Fill in details
    document.getElementById('execTime').textContent =
        data.executionTime + ' ms';

    document.getElementById('rowsScanned').textContent =
        data.rowsScanned.toLocaleString() + ' rows';

    document.getElementById('problem').textContent =
        data.problemExplanation;

    document.getElementById('fix').textContent =
        data.suggestedFix;

    document.getElementById('fixSQLBox').textContent =
        data.fixSQL || 'No SQL fix needed.';

    // Show report
    document.getElementById('report').style.display = 'block';
}

function copyFix() {
    const fixSQL = document.getElementById('fixSQLBox').textContent;
    navigator.clipboard.writeText(fixSQL);
    alert('Fix SQL copied to clipboard!');
}