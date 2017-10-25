<%@page import="java.math.BigDecimal"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="com.financeTracker.model.User"%>
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/functions" prefix = "fn" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Main | Finance Tracker</title>
<script src="js/Chart.bundle.js"></script>
<script src="js/utils.js"></script>
</head>
<body>
	<div>
		<jsp:include page="left.jsp"></jsp:include>
	</div>
	<div>
		<jsp:include page="header.jsp"></jsp:include>
	</div>
	<div class="content-wrapper">
		 <section class="content-header">
			<h2>Current balance across all accounts: <c:out value="${ balance }"></c:out></h2>
			<h1>All accounts</h1>
		</section>
		
		<section class="content">
			<div class="row">
			<c:forEach items="${ accounts }" var="account">
				<div class="col-lg-3 col-xs-6">
					<!-- small box -->
					<c:if test="${fn:contains(account.name, 'card')}">
			            <div class="small-box bg-yellow">
	            	</c:if>
	              	<c:if test="${fn:contains(account.name, 'Cash')}">
	            		<div class="small-box bg-green">
	            	</c:if>
	            	<c:if test="${fn:contains(account.name, 'Bank')}">
	            		<div class="small-box bg-red">
	            	</c:if>
	            	<c:if test="${!fn:contains(account.name, 'card') && !fn:contains(account.name, 'Cash') && !fn:contains(account.name, 'Bank')}">
	            		<div class="small-box bg-aqua">
	            	</c:if>
			            <div class="inner">
			              <h3><i class="ion-social-usd" style="font-size: 20px;"> </i><fmt:formatNumber value="${account.amount}" minFractionDigits="2"/></h3>
			              <p><c:out value="${account.name}"></c:out></p>
			            </div>
			            <div class="icon">
			            	<c:if test="${fn:contains(account.name, 'card')}">
			            	<div><i class="ion ion-card"></i></div>
			            	</c:if>
			              	<c:if test="${fn:contains(account.name, 'Cash')}">
			            		<div><i class="ion ion-cash"></i></div>
			            	</c:if>
			            	<c:if test="${fn:contains(account.name, 'Bank')}">
			            		<div><i class="ion ion-social-usd"></i></div>
			            	</c:if>
			            	<c:if test="${!fn:contains(account.name, 'card') && !fn:contains(account.name, 'Cash') && !fn:contains(account.name, 'Bank')}">
			            		<div><i class="ion ion-pie-graph"></i></div>
			            	</c:if>
			            </div>
			            <a href="transaction/account/${account.accountId}" class="small-box-footer">More info <i class="fa fa-arrow-circle-right"></i></a>
			          </div>
				</div>
			</c:forEach>
			
			<div class="col-lg-3 col-xs-6">
				<!-- small box -->
		          <div class="small-box bg-aqua">
		            <div class="inner">
		              <h3>Add</h3>
		              <p>Add new Account</p>
		            </div>
		            <div class="icon">
		             <div> <i class="ion ion-plus"></i></div>
		            </div>
		            <a href="addAccount" class="small-box-footer">Get started <i class="fa fa-arrow-circle-right"></i></a>
			          </div>
				</div>
			</div>
			
			<div>
				<c:set var="transactions" value="${ transactionsValues }" />
				<div style="width:100%; height: 100%">
			        <canvas id="canvas"></canvas>
			    </div>
				<script>
					var presets = window.chartColors;
					var utils = Samples.utils;
					var values = '${transactions}';
					
					values = values.replace(/[\[\]']+/g,'')
					
					var allTrans = [];
					$.each(values.split(","), function(i,e){
						allTrans.push(e);
					});
					
					var MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
				    var config = {
				        type: 'line',
				        data: {
				            labels: allTrans,
				            datasets: [{
				                label: "All transactions",
				                backgroundColor: utils.transparentize(presets.blue),
				                borderColor: window.chartColors.blue,
				                data: allTrans,
				                fill: true,
				            }]
				        },
				        options: {
				            responsive: true,
				            title:{
				                display:true,
				                text:'Balance chart'
				            },
				            tooltips: {
				                mode: 'index',
				                intersect: false,
				            },
				            hover: {
				                mode: 'nearest',
				                intersect: true
				            },
				            scales: {
				                xAxes: [{
				                    display: true,
				                    scaleLabel: {
				                        display: true,
				                        labelString: 'Month'
				                    }
				                }],
				                yAxes: [{
				                    display: true,
				                    scaleLabel: {
				                        display: true,
				                        labelString: 'Value'
				                    }
				                }]
				            }
				        }
				    };
				    
				    window.onload = function() {
				        var ctx = document.getElementById("canvas").getContext("2d");
				        window.myLine = new Chart(ctx, config);
				    };
				</script>
			</div>
	 	</section>
	</div>
	
	<div>
		<jsp:include page="footer.jsp"></jsp:include>
	</div>
	
<!-- jQuery 3 -->
<script src="js/jquery.min.js"></script>
<!-- chartJS utils -->
<script src="js/utils.js"></script>
<!-- Bootstrap 3.3.7 -->
<script src="js/bootstrap.min.js"></script>
<!-- SlimScroll -->
<script src="js/jquery.slimscroll.js"></script>
<!-- FastClick -->
<script src="js/fastclick.js"></script>
<!-- AdminLTE App -->
<script src="js/adminlte.min.js"></script>
<!-- AdminLTE for demo purposes -->
<script src="js/demo.js"></script>
</body>
</html>