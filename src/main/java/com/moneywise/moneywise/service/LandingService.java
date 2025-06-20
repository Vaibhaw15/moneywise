package com.moneywise.moneywise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moneywise.moneywise.entity.Category;
import com.moneywise.moneywise.entity.CategoryType;
import com.moneywise.moneywise.entity.Transaction;
import com.moneywise.moneywise.repository.CategoryRepository;
import com.moneywise.moneywise.repository.CategoryTypeRepository;
import com.moneywise.moneywise.repository.TransactionRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LandingService {

    @Autowired
    private TransactionRepository txnRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private CategoryTypeRepository categoryTypeRepo;

    private final DateTimeFormatter yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public Map<String, Object> getUserMonthlyStats(Integer userId, String startDateStr, String endDateStr) {
        Map<String, Object> result = new LinkedHashMap<>();

        LocalDate startDate = LocalDate.parse(startDateStr, yyyymmddFormatter);
        LocalDate endDate = LocalDate.parse(endDateStr, yyyymmddFormatter);

        Integer startDateInt = Integer.parseInt(startDate.format(yyyymmddFormatter));
        Integer endDateInt = Integer.parseInt(endDate.format(yyyymmddFormatter));

        // Previous month range
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth lastMonth = currentMonth.minusMonths(1);
        Integer startOfLastMonth = Integer.parseInt(lastMonth.atDay(1).format(yyyymmddFormatter));
        Integer endOfLastMonth = Integer.parseInt(lastMonth.atEndOfMonth().format(yyyymmddFormatter));

        List<Transaction> currentTransactions = txnRepo.findByUserIdAndTransactionDateIntBetween(userId, startDateInt,
                endDateInt);
        List<Transaction> lastMonthTransactions = txnRepo.findByUserIdAndTransactionDateIntBetween(userId,
                startOfLastMonth,
                endOfLastMonth);

        Map<Integer, Category> categoryIdMap = categoryRepo.findAll()
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<Integer, String> categoryTypeIdNameMap = categoryTypeRepo.findAll()
                .stream()

                .collect(Collectors.toMap(CategoryType::getId, CategoryType::getCategoryTypeName));

        Integer income = currentTransactions.stream()
                .filter(t -> {
                    Category category = categoryIdMap.get(t.getTransactionCategoryId());
                    if (category == null)
                        return false;
                    String type = categoryTypeIdNameMap.get(category.getCategoryTypeId());
                    return "Income".equalsIgnoreCase(type);
                })
                .mapToInt(Transaction::getTransactionAmount)
                .sum();

        Integer expense = currentTransactions.stream()
                .filter(t -> {
                    Category category = categoryIdMap.get(t.getTransactionCategoryId());
                    if (category == null)
                        return false;
                    String type = categoryTypeIdNameMap.get(category.getCategoryTypeId());
                    return "Expenses".equalsIgnoreCase(type);
                })
                .mapToInt(Transaction::getTransactionAmount)
                .sum();

        Integer currentBalance = income - expense; 

        // Last Month Income and Expense
        Integer lastIncome = lastMonthTransactions.stream()
                .filter(t -> {
                    Category category = categoryIdMap.get(t.getTransactionCategoryId());
                    if (category == null)
                        return false;
                    String type = categoryTypeIdNameMap.get(category.getCategoryTypeId());
                    return "Income".equalsIgnoreCase(type);
                })
                .mapToInt(Transaction::getTransactionAmount)
                .sum();

        Integer lastExpense = lastMonthTransactions.stream()
                .filter(t -> {
                    Category category = categoryIdMap.get(t.getTransactionCategoryId());
                    if (category == null)
                        return false;
                    String type = categoryTypeIdNameMap.get(category.getCategoryTypeId());
                    return "Expenses".equalsIgnoreCase(type);
                })
                .mapToInt(Transaction::getTransactionAmount)
                .sum();

        Integer lastBalance = lastIncome - lastExpense; // same here

        // Calculate difference and percentage
        Integer compareBalance = currentBalance - lastBalance;

        double comparePercentage;

        // Handle zero and negative cases safely
        if (lastBalance == 0) {
           if (currentBalance == 0) {
                    comparePercentage = 0.0;
                } else if (currentBalance > 0) {
                    comparePercentage = 100.0;
                } else {
                    comparePercentage = -100.0; 
                }
        } else if (lastBalance < 0 && currentBalance > 0) {
            // Recovering from negative to positive — treat denominator as absolute
            comparePercentage = (compareBalance * 100.0) / Math.abs(lastBalance);
        } else {
            comparePercentage = (compareBalance * 100.0) / lastBalance;
        }

        // ✅ Weekly Chart Data
        Map<String, Map<String, Integer>> weeklyMap = new LinkedHashMap<>();
        for (int i = 1; i <= 4; i++) {
            weeklyMap.put("Week " + i, new HashMap<>(Map.of("income", 0, "expense", 0)));
        }

        for (Transaction t : currentTransactions) {
            LocalDate date = LocalDate.parse(t.getTransactionDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int day = date.getDayOfMonth();
            int week = (day - 1) / 7 + 1;
            String key = "Week " + week;

            Category category = categoryIdMap.get(t.getTransactionCategoryId());
            if (category == null)
                continue;
            String type = categoryTypeIdNameMap.get(category.getCategoryTypeId());

            if ("Income".equalsIgnoreCase(type)) {
                weeklyMap.get(key).merge("income", t.getTransactionAmount(), Integer::sum);
            } else if ("Expenses".equalsIgnoreCase(type)) {
                weeklyMap.get(key).merge("expense", -t.getTransactionAmount(), Integer::sum);
            }
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String week = "Week " + i;
            chartData.add(Map.of(
                    "week", week,
                    "income", weeklyMap.get(week).get("income"),
                    "expense", weeklyMap.get(week).get("expense")));
        }

        result.put("income", income);
        result.put("expense", expense);
        result.put("currentBalance", currentBalance);
        result.put("compareBalanceFromLastMonth", compareBalance);
        result.put("compareBalanceFromLastMonthPercentage", comparePercentage);
        result.put("chartData", chartData);

        return result;
    }
}
