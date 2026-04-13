# AI Assistant Setup & Deployment Guide

**Version:** 2.5.0  
**Date:** April 4, 2026  
**Status:** ✅ READY FOR DEPLOYMENT

---

## Quick Start (5 minutes)

### Step 1: Get OpenAI API Key
1. Visit https://platform.openai.com/signup
2. Create an account or sign in
3. Go to API Keys section
4. Click "Create new secret key"
5. Copy the key (starts with `sk-`)

### Step 2: Set Environment Variable

**Windows (Command Prompt):**
```batch
setx OPENAI_API_KEY "sk-your-key-here"
# Restart Command Prompt or IDE for changes to take effect
```

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY = "sk-your-key-here"
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sk-your-key-here", "User")
# Restart PowerShell for changes to take effect
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY="sk-your-key-here"
# Add to ~/.bashrc or ~/.zshrc for persistent setting
```

### Step 3: Build & Run
```bash
cd C:\Users\ashis\IdeaProjects\db-explorer
mvn clean package
java -jar target/db-explorer-2.5.0.jar
```

### Step 4: Use AI Assistant
1. Launch DB Explorer
2. Connect to a database
3. Click "AI SQL Assistant" button in toolbar
4. Type your query description in natural language
5. Click "Generate SQL"
6. Copy and paste into a query tab

---

## Installation Checklist

- [ ] OpenAI account created
- [ ] API key generated and copied
- [ ] Environment variable set
- [ ] Java 17+ installed
- [ ] Maven 3.6+ installed
- [ ] Source code updated with new files
- [ ] pom.xml updated with httpclient5 dependency
- [ ] Project compiled successfully
- [ ] Application launched and tested
- [ ] AI Assistant button visible in toolbar
- [ ] Database connection selected
- [ ] Sample query generated successfully

---

## File Structure After Implementation

```
db-explorer/
├── pom.xml                                    (MODIFIED)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dbexplorer/
│   │   │       ├── service/
│   │   │       │   ├── AIAssistantService.java       (NEW)
│   │   │       │   └── ... (existing services)
│   │   │       └── ui/
│   │   │           ├── AIAssistantPanel.java         (NEW)
│   │   │           ├── MainFrame.java                (MODIFIED)
│   │   │           └── ... (existing UI classes)
│   │   └── resources/
│   │       └── app.properties                 (MODIFIED)
│   └── test/
│       └── java/
├── AI_ASSISTANT_FEATURE.md                    (NEW)
├── AI_ASSISTANT_SETUP_GUIDE.md               (NEW - THIS FILE)
└── ... (other files)
```

---

## Detailed Installation Steps

### Prerequisites Check

```bash
# Check Java version
java -version
# Output should be Java 17 or higher

# Check Maven version
mvn --version
# Output should be Maven 3.6 or higher

# Verify git (optional, for version control)
git --version
```

### Step-by-Step Installation

#### 1. Prepare the Source Code

Ensure you have all new files in the correct locations:

- ✅ `src/main/java/com/dbexplorer/service/AIAssistantService.java` (NEW)
- ✅ `src/main/java/com/dbexplorer/ui/AIAssistantPanel.java` (NEW)
- ✅ `pom.xml` - Modified with httpclient5 dependency
- ✅ `src/main/resources/app.properties` - Modified with AI settings
- ✅ `src/main/java/com/dbexplorer/ui/MainFrame.java` - Modified with AI integration

#### 2. Update Dependencies

Edit `pom.xml` and verify httpclient5 is present:

```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.3.1</version>
</dependency>
```

#### 3. Configure Environment

**Windows:**
```batch
# Set the API key
setx OPENAI_API_KEY "sk-your-actual-key-here"

# Verify it's set
echo %OPENAI_API_KEY%
# Should output: sk-your-actual-key-here
```

**Linux/Mac:**
```bash
# Set the API key
export OPENAI_API_KEY="sk-your-actual-key-here"

# Make it permanent (add to ~/.bashrc or ~/.zshrc)
echo 'export OPENAI_API_KEY="sk-your-actual-key-here"' >> ~/.bashrc
source ~/.bashrc

# Verify it's set
echo $OPENAI_API_KEY
# Should output: sk-your-actual-key-here
```

#### 4. Build the Project

```bash
cd C:\Users\ashis\IdeaProjects\db-explorer

# Clean and build
mvn clean package -DskipTests

# Full build with tests
mvn clean package
```

**Expected Output:**
```
[INFO] Building db-explorer 2.5.0
[INFO] --------------------------------
[INFO] BUILD SUCCESS
[INFO] --------------------------------
[INFO] Total time: X.XXs
[INFO] Finished at: YYYY-MM-DDTHH:MM:SS±HH:MM
```

#### 5. Run the Application

```bash
# Method 1: Using JAR directly
java -jar target/db-explorer-2.5.0.jar

# Method 2: Using Maven
mvn spring-boot:run

# Method 3: From IDE (if using IntelliJ IDEA)
# Run → Edit Configurations → Add new configuration
# Main class: com.dbexplorer.App
```

#### 6. Verify Installation

Once the application launches:

1. **Check Toolbar:**
   - Look for "AI SQL Assistant" button in the toolbar
   - Should be visible between "Toggle Health Dashboard" and "Theme" selector

2. **Check Configuration Status:**
   - Click "AI SQL Assistant" button
   - Should see green checkmark: "✓ AI Assistant is configured and ready to use"
   - If red warning, verify environment variable is set correctly

3. **Test with Sample Data:**
   - Connect to any database
   - Click "AI SQL Assistant"
   - Try: "Show me all tables"
   - Should generate: `SELECT table_name FROM information_schema.tables;`

---

## Troubleshooting Installation

### Problem: "Cannot find symbol: AIAssistantService"

**Cause:** New Java files not in correct location
**Solution:**
```
1. Verify files are in:
   - src/main/java/com/dbexplorer/service/AIAssistantService.java
   - src/main/java/com/dbexplorer/ui/AIAssistantPanel.java
2. Reload IDE project (Maven → Reload Projects)
3. Clean build: mvn clean compile
```

### Problem: "httpclient5 not found"

**Cause:** Maven not finding dependency
**Solution:**
```
1. Check internet connectivity
2. Clear Maven cache: rm -rf ~/.m2/repository
3. Re-download: mvn clean dependency:resolve
4. Rebuild: mvn clean package
```

### Problem: "OPENAI_API_KEY environment variable not set"

**Cause:** Environment variable not properly configured
**Solution:**
```batch
# Windows - Restart IDE after setting
setx OPENAI_API_KEY "sk-..."
# Close and reopen IDE completely

# Linux/Mac - Source bashrc
source ~/.bashrc
# or restart terminal
```

### Problem: AI Button Not Showing

**Cause:** MainFrame not properly updated
**Solution:**
1. Verify MainFrame.java has the openAIAssistant() method
2. Verify the aiBtn line is in initToolbar()
3. Clean rebuild: `mvn clean package`
4. Restart application

### Problem: "Class not found" errors

**Cause:** IDE hasn't reloaded Maven configuration
**Solution:**
- IntelliJ: File → Invalidate Caches → Restart
- Eclipse: Project → Clean → Build All
- Maven: `mvn clean install`

---

## Version Update Instructions

### From Version 2.4.1 to 2.5.0

#### 1. Backup Current Installation
```bash
# Backup current db-explorer
cp -r db-explorer db-explorer-2.4.1-backup
```

#### 2. Update Source Code
```bash
# Add new files to your source directory
# Copy AIAssistantService.java to src/main/java/com/dbexplorer/service/
# Copy AIAssistantPanel.java to src/main/java/com/dbexplorer/ui/
```

#### 3. Update Configuration Files
```bash
# Update pom.xml (add httpclient5 dependency)
# Update app.properties (add ai.enabled and ai.model)
# Update MainFrame.java (add AI integration)
```

#### 4. Build New Version
```bash
mvn clean package
# Generates: target/db-explorer-2.5.0.jar
```

#### 5. Deploy
```bash
# Replace old JAR with new one
cp target/db-explorer-2.5.0.jar /path/to/deployment/
```

---

## Configuration Options

### app.properties Settings

```properties
# AI Assistant Feature Flags
ai.enabled=true                    # Enable/disable feature
ai.model=gpt-3.5-turbo             # Model choice: gpt-3.5-turbo or gpt-4

# Existing settings still apply
query.max.rows=10000
query.fetch.size=500
export.fetch.size=500
```

### Environment Variables

```
OPENAI_API_KEY=sk-...              # Required for AI to work
JAVA_OPTS=-Xmx2048m               # Recommended heap size
```

---

## Performance Optimization

### Recommended Settings

```properties
# For optimal performance
ai.model=gpt-3.5-turbo             # Faster and cheaper than gpt-4
query.fetch.size=500               # Balanced batch size
query.max.rows=10000               # Prevents memory issues
```

### System Requirements

**Minimum:**
- RAM: 512 MB
- CPU: Dual core
- Network: 1 Mbps

**Recommended:**
- RAM: 2 GB
- CPU: Quad core
- Network: 10 Mbps
- SSD for database files

### Network Requirements

- **Internet Connectivity:** Required for OpenAI API calls
- **Bandwidth:** ~100 KB per API call
- **Latency:** <100ms preferred
- **OpenAI Status:** https://status.openai.com

---

## Testing After Installation

### Basic Functionality Test

```
1. Launch DB Explorer
2. Connect to test database (PostgreSQL, MySQL, etc.)
3. Click "AI SQL Assistant" button
4. Verify dialog opens
5. Verify schema is loaded (should see tables listed)
6. Enter: "Get all records from the first table"
7. Click "Generate SQL"
8. Verify SQL is generated (e.g., "SELECT * FROM table_name;")
9. Click "Copy to Clipboard"
10. Go to Query tab, paste (Ctrl+V)
11. Run query (Ctrl+Enter)
12. Verify results display correctly
```

### Advanced Test Scenarios

**Test Case 1: Complex Join Query**
```
Input: "Show customers with their total purchase amount"
Expected: SELECT statement with JOIN and SUM
Result: ✅ PASS / ❌ FAIL
```

**Test Case 2: Error Handling**
```
Input: "Show me all secret data" (vague request)
Expected: Either generates query or shows error message
Result: ✅ PASS / ❌ FAIL
```

**Test Case 3: Different Databases**
```
Input: "List all users" (for different database types)
Expected: Database-specific SQL syntax
PostgreSQL: SELECT * FROM users;
MySQL: SELECT * FROM users;
SQL Server: SELECT * FROM [users];
Result: ✅ PASS / ❌ FAIL
```

---

## Monitoring & Maintenance

### Monitor API Usage

1. Go to https://platform.openai.com/account/usage
2. Check token usage and costs
3. Set billing limits if needed
4. Review quota usage

### Regular Maintenance

**Weekly:**
- Check API costs (aim for <$10/week per user)
- Review error logs for patterns
- Monitor response times (should be <5s)

**Monthly:**
- Review API usage patterns
- Optimize common queries
- Update to latest OpenAI model if available
- Rotate API keys

---

## Security Hardening

### API Key Protection

1. **Never commit keys to Git:**
   ```bash
   echo "OPENAI_API_KEY=*" >> .gitignore
   ```

2. **Use environment variables:**
   ```bash
   # Set in secure location, not hardcoded
   setx OPENAI_API_KEY "sk-..."
   ```

3. **Rotate keys regularly:**
   - Quarterly API key rotation
   - Immediately if suspected leak
   - Archive old keys safely

4. **Monitor usage:**
   - Check OpenAI dashboard daily
   - Alert on unusual activity
   - Set spend limits

### Data Privacy

1. **Schema only, no data:**
   - AI only sees table/column names
   - No actual data rows transmitted
   - Safe for sensitive databases

2. **Review OpenAI policies:**
   - Read: https://openai.com/policies/privacy-policy
   - Review: https://openai.com/policies/terms-of-use
   - Ensure compliance with your organization

3. **Encrypt communications:**
   - HTTPS used for all API calls
   - TLS 1.3+ enforced
   - End-to-end encrypted

---

## Support & Resources

### Documentation
- [AI_ASSISTANT_FEATURE.md](AI_ASSISTANT_FEATURE.md) - Feature guide
- [AI_ASSISTANT_SETUP_GUIDE.md](AI_ASSISTANT_SETUP_GUIDE.md) - This file
- README.md - General DB Explorer docs

### External Resources
- OpenAI API Docs: https://platform.openai.com/docs
- DB Explorer GitHub: [repository-url]
- Community Support: [forum-url]

### Contact Support
- Email: adeptashish@gmail.com
- Issue Tracker: [github-issues-url]
- Chat: [discord-url]

---

## Rollback Instructions

If you need to revert to version 2.4.1:

```bash
# Stop the application

# Restore backup
rm -rf db-explorer
mv db-explorer-2.4.1-backup db-explorer

# Or rebuild from 2.4.1 source
git checkout v2.4.1
mvn clean package
java -jar target/db-explorer-2.4.1.jar
```

---

## Success Criteria

Your installation is successful when:

✅ Application launches without errors  
✅ "AI SQL Assistant" button visible in toolbar  
✅ AI Assistant dialog opens and displays schema  
✅ Can generate SQL from natural language input  
✅ Generated SQL can be copied to clipboard  
✅ Can paste and execute in query tabs  
✅ No OPENAI_API_KEY errors  
✅ Configuration status shows "configured and ready"  

---

## Next Steps

1. **Try Sample Queries:**
   - Simple: "Show all users"
   - Medium: "Get users with orders in last month"
   - Complex: "Show top 10 customers by total spend"

2. **Explore Features:**
   - Test copy/paste functionality
   - Try different query types
   - Experiment with descriptions

3. **Read Documentation:**
   - Review AI_ASSISTANT_FEATURE.md for details
   - Check troubleshooting section
   - Understand limitations

4. **Optimize Usage:**
   - Use gpt-3.5-turbo for cost efficiency
   - Batch similar queries
   - Monitor API costs

---

## Conclusion

The AI SQL Assistant is now installed and ready to use! Follow the quick start guide above and you'll be generating SQL queries from natural language in minutes.

For any issues or questions, refer to the troubleshooting section or contact support.

**Happy querying with AI! 🚀**

---

*Document created: April 4, 2026*  
*DB Explorer Version: 2.5.0*  
*Last updated: April 4, 2026*

