package model.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Account;
import model.Budget;
import model.Category;
import model.Tag;
import model.Transaction;
import model.TransactionType;

public class TransactionDAO {
	private static TransactionDAO instance;
	private static final HashMap<TransactionType, ArrayList<Transaction>> ALL_TRANSACTIONS = new HashMap<>();
	private static final Connection CONNECTION = DBManager.getInstance().getConnection();
	
	private TransactionDAO() throws SQLException {
		ALL_TRANSACTIONS.put(TransactionType.EXPENCE, new ArrayList<>());
		ALL_TRANSACTIONS.put(TransactionType.INCOME, new ArrayList<>());
		getAllTransactions();
	}
	
	public synchronized static TransactionDAO getInstance() throws SQLException {
		if (instance == null) {
			instance = new TransactionDAO();
		}
		return instance;
	}
	
	public synchronized void getAllTransactions() throws SQLException {
		String query = "SELECT transaction_id, type, date, description, amount, account_id, category_id FROM finance_tracker.transactions";
		PreparedStatement statement = null;
		statement = CONNECTION.prepareStatement(query);
		ResultSet result = statement.executeQuery();
		while (result.next()) {
			long transactionId = result.getInt("transaction_id");
			String type = result.getString("type");
			TransactionType transactionType = TransactionType.valueOf(type);
			LocalDateTime date = result.getTimestamp("date").toLocalDateTime();
			BigDecimal amount = result.getBigDecimal("amount");
			String description = result.getString("description");
			int accountId = result.getInt("account_id");
			int categoryId = result.getInt("category_id");
			HashSet<Tag> tags = TagDAO.getInstance().getTagsByTransactionId(transactionId);
			String categoryName = CategoryDAO.getInstance().getCategoryNameByCategoryId(categoryId);
			Transaction t = new Transaction(transactionId, transactionType, description, amount, accountId, categoryId, date, tags);
			t.setCategoryName(categoryName);
			ALL_TRANSACTIONS.get(t.getType()).add(t);
		}
	}

	public synchronized List<Transaction> getAllTransactionsByAccountId(long accountId) {
		List<Transaction> transactions = new ArrayList<Transaction>();
		for (ArrayList<Transaction> transactionTypes : ALL_TRANSACTIONS.values()) {
			for (Transaction transaction : transactionTypes) {
				if (transaction.getAccount() == accountId) {
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	public synchronized List<Transaction> getAllTransactionsByCategoryId(long categoryId) {
		List<Transaction> transactions = new ArrayList<Transaction>();
		for (ArrayList<Transaction> transactionTypes : ALL_TRANSACTIONS.values()) {
			for (Transaction transaction : transactionTypes) {
				if (transaction.getCategory() == categoryId) {
					transactions.add(transaction);
				}
			}
		}
		return transactions;
	}
	
	
	public synchronized Transaction getTransactionByTransactionId(long transactionId) throws SQLException {
		String sql = "SELECT type, date, description, amount, account_id, category_id FROM transactions WHERE transaction_id = ?;";
		
		PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql);
		ps.setLong(1, transactionId);
		
		ResultSet res = ps.executeQuery();
		res.next();
		
		TransactionType transactionType = TransactionType.valueOf(res.getString("type"));
		LocalDateTime date = res.getTimestamp("date").toLocalDateTime();
		String description = res.getString("description");
		BigDecimal amount = res.getBigDecimal("amount");
		int accountId = res.getInt("account_id");
		int categoryId = res.getInt("category_id");
		HashSet<Tag> tags = TagDAO.getInstance().getTagsByTransactionId(transactionId);
		
		Transaction t = new Transaction(transactionId, transactionType, description, amount, accountId, categoryId, date, tags);
		
		return t;
	}
	
	public synchronized void insertTransaction(Transaction t) throws SQLException {
		String query = "INSERT INTO finance_tracker.transactions (type, date, amount, description, account_id, category_id) VALUES (?, STR_TO_DATE(?, '%Y-%m-%d %H:%i:%s'), ?, ?, ?, ?)";
		PreparedStatement statement = CONNECTION.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		statement.setString(1, t.getType().toString());
		statement.setTimestamp(2, Timestamp.valueOf(t.getDate().withNano(0)));
		statement.setBigDecimal(3, t.getAmount());
		statement.setString(4, t.getDescription());
		statement.setLong(5, t.getAccount());
		statement.setLong(6, t.getCategory());
		statement.executeUpdate();
		
		ResultSet resultSet = statement.getGeneratedKeys();
		resultSet.next();
		t.setTransactionId(resultSet.getLong(1));
		try {
			CONNECTION.setAutoCommit(false);
			
			for (Tag tag : t.getTags()) {
				TagDAO.getInstance().insertTagToTags(tag, tag.getUserId());
				TagDAO.getInstance().insertTagToTransaction(t, tag);
			}
			
			boolean existsBudget = BudgetDAO.getInstance().existsBudget(t.getDate(), t.getCategory(), t.getAccount());
			System.out.println(existsBudget);
			Set<Budget> budgets =  BudgetDAO.getInstance().getAllBudgetsByDateCategoryAndAccount(t.getDate(), t.getCategory(), t.getAccount());
			if (existsBudget) {
				for (Budget budget : budgets) {
					BudgetsHasTransactionsDAO.getInstance().insertTransactionBudget(budget.getBudgetId(), t.getTransactionId());
					if (t.getType().equals(TransactionType.EXPENCE)) {
						budget.setAmount(budget.getAmount().add(t.getAmount()));
					}
					BudgetDAO.getInstance().updateBudget(budget);
					System.out.println("aliluq");
				}
			}
			
			ALL_TRANSACTIONS.get(t.getType()).add(t);
			CONNECTION.commit();
		} catch (SQLException e) {
			CONNECTION.rollback();
			throw new SQLException();
		} finally {
			CONNECTION.setAutoCommit(true);
		}
	}
	
	public synchronized void updateTransaction(Transaction t) throws SQLException {
		String query = "UPDATE finance_tracker.transactions SET type = ?, date = STR_TO_DATE(?, '%Y-%m-%d %H:%i:%s'), amount = ?, account_id = ?, category_id = ? WHERE transaction_id = ?";
		PreparedStatement statement = CONNECTION.prepareStatement(query);
		statement.setString(1, t.getType().toString());
		statement.setTimestamp(2, Timestamp.valueOf(t.getDate().withNano(0)));
		statement.setBigDecimal(3, t.getAmount());
		statement.setLong(4, t.getAccount());
		statement.setLong(5, t.getCategory());
		statement.setLong(7, t.getTransactionId());
		statement.executeUpdate();
		
		ALL_TRANSACTIONS.get(t.getType()).add(t);
	}
	
	public synchronized void deleteTransaction(Transaction t) throws SQLException {
		try {
			CONNECTION.setAutoCommit(false);
			BudgetsHasTransactionsDAO.getInstance().deleteTransactionBudgetByTransactionId(t.getTransactionId());
			
			String query = "DELETE FROM finance_tracker.transactions WHERE transaction_id = ?";
			PreparedStatement statement = CONNECTION.prepareStatement(query);
			statement.setLong(1, t.getTransactionId());
			statement.executeUpdate();
			
			ALL_TRANSACTIONS.get(t.getType()).remove(t);
			CONNECTION.commit();
		} catch (SQLException e) {
			CONNECTION.rollback();
			
			throw new SQLException();
		} finally {
			CONNECTION.setAutoCommit(true);
		}
	}
	
	public synchronized void removeTransaction(Transaction t) {
		ALL_TRANSACTIONS.get(t.getType()).remove(t);
	}

	public boolean existsTransaction(LocalDateTime fromDate, LocalDateTime toDate, long categoryId, long accountId) throws SQLException {
		String sql = "SELECT type, date, account_id, category_id FROM transactions WHERE category_id = ? AND account_id = ?;";
		
		PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql);
		ps.setLong(1, categoryId);
		ps.setLong(2, accountId);
		
		ResultSet res = ps.executeQuery();
		
		while (res.next()) {
			TransactionType type = TransactionType.valueOf(res.getString("type"));
			LocalDateTime date = res.getTimestamp("date").toLocalDateTime();
			
			if (type.equals(TransactionType.EXPENCE) && isBetweenTwoDates(date, fromDate, toDate)) {
				return true;
			}
		}
		
		return false;
	}

	private boolean isBetweenTwoDates(LocalDateTime date, LocalDateTime from, LocalDateTime to) {
		return !date.isBefore(from) && !date.isAfter(to);
	}

	public Set<Transaction> getAllTransactionsForBudget(LocalDateTime fromDate, LocalDateTime toDate, long categoryId,
			long accountId) throws SQLException {
		String sql = "SELECT transaction_id, type, date, description, amount, account_id, category_id FROM transactions WHERE category_id = ? AND account_id = ?;";
		
		PreparedStatement ps = DBManager.getInstance().getConnection().prepareStatement(sql);
		ps.setLong(1, categoryId);
		ps.setLong(2, categoryId);
		
		ResultSet res = ps.executeQuery();
		
		Set<Transaction> transactions = new HashSet<>();
		
		while(res.next()) {
			long transactionId = res.getLong("transaction_id");
			TransactionType type = TransactionType.valueOf(res.getString("type"));
			LocalDateTime date = res.getTimestamp("date").toLocalDateTime();
			String description = res.getString("description");
			BigDecimal amount = res.getBigDecimal("amount");
			
			if (type.equals(TransactionType.EXPENCE) && isBetweenTwoDates(date, fromDate, toDate)) {
				Transaction t = new Transaction(transactionId, type, description, amount, accountId, categoryId, date, null);
				
				transactions.add(t);
			}
		}
		
		return transactions;
	}
}
