package controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Account;
import model.Tag;
import model.Transaction;
import model.TransactionType;
import model.User;
import model.db.AccountDAO;
import model.db.TransactionDAO;

@WebServlet("/transaction")
public class TransactionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		List<Transaction> transactions = null;
		BigDecimal accountBalance = null;
		String accountName = null;
		try {
			long accountId = Long.valueOf(request.getParameter("accountId"));
			transactions = TransactionDAO.getInstance().getAllTransactionsByAccountId(accountId);
			accountBalance = AccountDAO.getInstance().getAmountByAccountId(accountId);
			accountName = AccountDAO.getInstance().getAccountNameByAccountId(accountId);
		} catch (SQLException e) {
			System.out.println("neshto katastrofalno se slu4i");
			e.printStackTrace();
		}
		
		String balance = NumberFormat.getCurrencyInstance().format(accountBalance);
		request.getSession().setAttribute("accountName", accountName);
		request.getSession().setAttribute("balance", balance);
		request.getSession().setAttribute("transactions", transactions);
		request.getRequestDispatcher("transactions.jsp").forward(request, response);
		
//		for (Transaction transaction : trans) {
//			response.getWriter().append("<h4>" + transaction.toString() + "</h4>");
//		}
//		
//		response.getWriter().append("Served at: ").append(request.getContextPath());
		//response.sendRedirect("result.html");
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//doGet(request, response);
		String type = request.getParameter("type");
		String amount = request.getParameter("amount");
		String account = request.getParameter("account");
		String category = request.getParameter("category");
		String tags = request.getParameter("tags");
		String description = request.getParameter("description");
		
		User u = (User) request.getSession().getAttribute("user");
		HashSet<Tag> tagsSet = new HashSet<>();
		if (!tags.isEmpty()) {
			String[] tagNames = tags.split(" ");
			for (String tag : tagNames) {
				tagsSet.add(new Tag(tag, u.getUserId()));
			}
		}
		
		Transaction t = new Transaction(TransactionType.valueOf(type), description, BigDecimal.valueOf(Double.valueOf(amount)), Long.parseLong(account), Long.parseLong(category), LocalDateTime.now(), tagsSet);
		try {
			Account acc = AccountDAO.getInstance().getAccountByAccountId(Long.parseLong(account));
			BigDecimal newValue = BigDecimal.valueOf(Double.valueOf(amount));
			BigDecimal oldValue = AccountDAO.getInstance().getAmountByAccountId((int)acc.getAccountId());
			if (type.equals("EXPENCE")) {
				AccountDAO.getInstance().updateAccountAmount(acc, (oldValue.subtract(newValue)));
			} else 
			if (type.equals("INCOME")) {
				AccountDAO.getInstance().updateAccountAmount(acc, (oldValue.add(newValue)));
			}
			TransactionDAO.getInstance().insertTransaction(t);
		} catch (SQLException e) {
			System.out.println("neshto katastrofalno se slu4i");
			e.printStackTrace();
		}
		//request.setAttribute("id", Long.parseLong(account));
		//doGet(request, response);
	}

}
