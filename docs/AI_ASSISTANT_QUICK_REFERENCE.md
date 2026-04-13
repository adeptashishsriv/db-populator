# AI SQL Assistant - Quick Reference Guide

**Version:** 2.5.0  
**Date:** April 4, 2026

---

## One-Page Quick Reference

### Setup (Do This Once)
1. Get API key: https://platform.openai.com/api_keys
2. Set environment variable: `OPENAI_API_KEY=sk-your-key`
3. Restart DB Explorer
4. Done! ✅

### Using the AI Assistant

**Step 1:** Open AI Assistant
```
Click: Toolbar → AI SQL Assistant button
```

**Step 2:** Select Database
```
Left panel → Click on a database connection
(AI loads schema automatically)
```

**Step 3:** Describe Your Query
```
Left text area → Type what you want in plain English
Example: "Get all customers from California with orders over $1000"
```

**Step 4:** Generate SQL
```
Click: Generate SQL button
(Wait 2-5 seconds for AI to respond)
```

**Step 5:** Use the SQL
```
Option A: Copy to Clipboard → Paste in Query tab (Ctrl+V)
Option B: Insert into Query Tab → Automatically paste
```

**Step 6:** Execute
```
Query Tab → Press Ctrl+Enter or click Run button
```

---

## Examples by Query Type

### Simple Queries
| What You Say | Generated SQL |
|---|---|
| "Show all products" | `SELECT * FROM products;` |
| "List customers from London" | `SELECT * FROM customers WHERE city='London';` |
| "Get expensive items" | `SELECT * FROM products WHERE price > 100;` |

### Join Queries
| What You Say | Generated SQL |
|---|---|
| "Show customers and their orders" | `SELECT c.*, o.* FROM customers c JOIN orders o ON c.id=o.customer_id;` |
| "Find users who bought product X" | `SELECT u.* FROM users u JOIN orders o ON u.id=o.user_id WHERE o.product_id=X;` |

### Aggregate Queries
| What You Say | Generated SQL |
|---|---|
| "Total sales per month" | `SELECT DATE_TRUNC('month', date) AS month, SUM(amount) FROM sales GROUP BY month;` |
| "Count of orders per customer" | `SELECT customer_id, COUNT(*) FROM orders GROUP BY customer_id;` |
| "Average price by category" | `SELECT category, AVG(price) FROM products GROUP BY category;` |

### Filter & Sort
| What You Say | Generated SQL |
|---|---|
| "Top 10 customers by spending" | `SELECT customer_id, SUM(amount) AS total FROM orders GROUP BY customer_id ORDER BY total DESC LIMIT 10;` |
| "Recent orders from last week" | `SELECT * FROM orders WHERE order_date >= NOW() - INTERVAL '7 days' ORDER BY order_date DESC;` |

---

## Common Patterns

### Pattern 1: List All
**Say:** "Show all [table name]"
**Result:** `SELECT * FROM [table_name];`

### Pattern 2: Filter by Value
**Say:** "Get [table] where [column] is [value]"
**Result:** `SELECT * FROM [table] WHERE [column] = '[value]';`

### Pattern 3: Count Records
**Say:** "How many [records] are there"
**Result:** `SELECT COUNT(*) FROM [table];`

### Pattern 4: Group & Aggregate
**Say:** "Total [amount] per [column]"
**Result:** `SELECT [column], SUM([amount]) FROM [table] GROUP BY [column];`

### Pattern 5: Join Tables
**Say:** "[Table1] with their [table2]"
**Result:** `SELECT * FROM [table1] JOIN [table2] ON [join_condition];`

---

## Tips for Best Results

### ✅ DO
- ✅ Be specific: "Orders > $100" (not "expensive orders")
- ✅ Use column names if you know them
- ✅ Mention time periods: "last month", "this year", "since 2025"
- ✅ Include aggregations: "total", "average", "count"
- ✅ Specify sorting: "highest", "most recent", "alphabetical"
- ✅ Ask for limits: "top 10", "first 5"

### ❌ DON'T
- ❌ Don't use vague terms: "stuff", "things", "data"
- ❌ Don't ask for sensitive operations: INSERT, DELETE, DROP
- ❌ Don't include actual data in queries: "password='abc123'"
- ❌ Don't ask for complex business logic
- ❌ Don't ask for non-SQL operations

---

## Troubleshooting

### Problem: "OPENAI_API_KEY not set"
**Solution:** 
```
Windows: setx OPENAI_API_KEY "sk-..."
Linux/Mac: export OPENAI_API_KEY="sk-..."
Restart DB Explorer
```

### Problem: "Connection timeout"
**Solution:**
- Check internet connection
- Try again (might be API issue)
- Check OpenAI status: https://status.openai.com

### Problem: "Generated SQL has errors"
**Solution:**
- Review the schema (shown in dialog)
- Refine your description
- Try a simpler query
- Check database-specific syntax

### Problem: "No schema loading"
**Solution:**
- Make sure database is connected
- Check user permissions
- Try different database
- Restart application

### Problem: "Generated SQL is wrong"
**Solution:**
- Be more specific in description
- Mention table/column names explicitly
- Ask for a simpler query first
- Review AI limitations below

---

## What AI Can Do

✅ SELECT queries  
✅ JOINs between tables  
✅ WHERE conditions  
✅ GROUP BY aggregations  
✅ ORDER BY sorting  
✅ LIMIT results  
✅ Subqueries  
✅ Window functions  
✅ Complex conditions  

---

## What AI Cannot Do

❌ INSERT/UPDATE/DELETE (safety)  
❌ CREATE/ALTER/DROP (safety)  
❌ Complex stored procedures  
❌ Database-specific PL/SQL  
❌ Real-time data updates  
❌ Performance optimization (yet)  

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Copy to Clipboard | Ctrl+C (in output area) |
| Paste to Query Tab | Ctrl+V (in query area) |
| Generate SQL | Enter (if focus in input) |
| Run Query | Ctrl+Enter (in query tab) |
| New Query Tab | Ctrl+N |

---

## Keyboard Shortcuts in AI Panel

| Action | Shortcut |
|--------|----------|
| Generate SQL | Click Button or Alt+G |
| Copy | Click Button or Ctrl+C |
| Clear Input | Click Button or Ctrl+A, Delete |

---

## Cost Information

### Per-Query Cost
- Average: ~$0.0004 (less than half a cent)
- Range: $0.0001 - $0.001 per query
- Fast queries cost less than slow queries

### Monthly Estimates
```
10 queries/day:     ~$0.10/month
50 queries/day:     ~$0.50/month
100 queries/day:    ~$1.00/month
```

### Cost Control
- Monitor usage: https://platform.openai.com/account/usage
- Set spending limit in OpenAI dashboard
- Use GPT-3.5-turbo (cheaper than GPT-4)
- Batch similar queries

---

## Best Practices

### Practice 1: Plan Your Query
```
❌ Bad: "Show me data"
✅ Good: "Show all customers from California who purchased in the last 30 days"
```

### Practice 2: Be Specific
```
❌ Bad: "Total sales"
✅ Good: "Total sales by product category for this month"
```

### Practice 3: Verify Generated SQL
```
Always review SQL before executing
Check table/column names match your database
Run on test data first if complex
```

### Practice 4: Use Database Knowledge
```
If you know table names: Use them
If you know relationships: Mention them
If you know constraints: Reference them
```

### Practice 5: Keep It Simple
```
Complex queries = More errors = More refining
Start simple, add complexity
Test each step
```

---

## Database-Specific Notes

### PostgreSQL
- Full support ✅
- Uses INTERVAL, DATE_TRUNC, etc.
- Supports JSON/JSONB queries

### MySQL
- Full support ✅
- Uses DATE_ADD, DATE_SUB
- MySQL-specific functions work

### SQL Server
- Full support ✅
- Bracket notation: [column_name]
- DATEADD, DATEDIFF functions

### Oracle
- Full support ✅
- Uses SYSDATE, TRUNC
- Oracle-specific functions supported

### SQLite
- Full support ✅
- Limited function support
- Simple queries work best

---

## Performance Tips

### Tip 1: Faster Responses
- Use GPT-3.5-turbo (default)
- Keep descriptions concise
- Avoid very large schemas

### Tip 2: Better Results
- Know your schema
- Use actual column/table names
- Be specific about requirements
- Review and refine iteratively

### Tip 3: Cost Savings
- Batch similar queries
- Use simpler descriptions
- Reuse generated SQL
- Cache common patterns

---

## Getting Help

### Documentation
- Feature Guide: [AI_ASSISTANT_FEATURE.md](AI_ASSISTANT_FEATURE.md)
- Setup Guide: [AI_ASSISTANT_SETUP_GUIDE.md](AI_ASSISTANT_SETUP_GUIDE.md)
- Implementation Details: [AI_ASSISTANT_IMPLEMENTATION_SUMMARY.md](AI_ASSISTANT_IMPLEMENTATION_SUMMARY.md)

### Online Resources
- OpenAI Docs: https://platform.openai.com/docs
- DB Explorer GitHub: [repository-url]
- SQL Tutorial: https://sqlzoo.net

### Support
- Email: adeptashish@gmail.com
- Issue Tracker: [github-url]/issues
- Community: [forum-url]

---

## Limitations to Know

1. **Not Real-time:** Schema extracted when you connect
2. **AI Hallucinations:** Might generate non-existent columns
3. **Index Unaware:** Doesn't know about your indexes
4. **Cost:** Every query has a small cost
5. **Rate Limits:** API has usage limits
6. **No Multi-DB:** Can't join across different databases
7. **No Security Functions:** Won't generate password-related queries

---

## Advanced Usage

### Iterative Refinement
```
1. Ask: "Show top 10 customers"
2. Got: SELECT * FROM customers LIMIT 10;
3. Refine: "Top 10 customers by total spending"
4. Got: Better query with JOIN and SUM
5. Perfect!
```

### Combining with Query Editor
```
1. Generate simple SELECT in AI
2. Manually add WHERE conditions
3. Add ORDER BY for sorting
4. Test and refine
```

### Saving Generated Queries
```
1. Copy from AI Assistant
2. Paste in Query tab
3. Save query (Ctrl+S)
4. Reuse later
```

---

## Frequently Asked Questions

**Q: Is my data sent to OpenAI?**  
A: No, only schema metadata (table/column names), not actual data.

**Q: Can I use this offline?**  
A: No, requires internet connection to OpenAI API.

**Q: Can it generate DELETE/INSERT?**  
A: No, for safety reasons. Only SELECT queries.

**Q: How much does it cost?**  
A: ~$0.0004 per query (~$0.10-$1.00 per month).

**Q: Can I use GPT-4 instead?**  
A: Yes, change ai.model in app.properties (costs more).

**Q: What if generated SQL is wrong?**  
A: Review schema, refine description, try again.

**Q: Can it optimize my queries?**  
A: Not yet, but that's planned for future versions.

---

## Quick Command Reference

| Goal | What To Say |
|------|-----------|
| See all records | "Show all [table]" |
| Filter data | "[table] where [column] = [value]" |
| Count records | "How many [table] are there" |
| Group data | "Total [amount] by [column]" |
| Join tables | "[table1] with their [table2]" |
| Sort results | "[table] ordered by [column]" |
| Top records | "Top [N] [table] by [column]" |
| Date filtering | "[table] from [last month/year]" |

---

## Summary Checklist

- [ ] API key obtained from OpenAI
- [ ] Environment variable set (OPENAI_API_KEY)
- [ ] DB Explorer restarted
- [ ] Database connected
- [ ] AI Assistant button visible in toolbar
- [ ] Dialog opens when button clicked
- [ ] Schema appears in dialog
- [ ] First query generated successfully
- [ ] SQL copied to clipboard
- [ ] Query executed in query tab
- [ ] Results displayed correctly

---

## Ready to Go!

You're all set! Start asking questions in natural language and let AI generate your SQL. Remember:

1. **Be specific** in your descriptions
2. **Review generated SQL** before executing
3. **Monitor API costs** in OpenAI dashboard
4. **Report issues** if queries don't work

**Happy querying! 🚀**

---

*Quick Reference Guide - DB Explorer v2.5.0*  
*Created: April 4, 2026*

