/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */

package com.finsight.backend.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.finsight.backend.dto.response.UploadResponse;
import com.finsight.backend.dto.transaction.TransactionResponse;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.PaymentMethod;
import com.finsight.backend.enums.TransactionCategory;
import com.finsight.backend.enums.TransactionType;
import com.finsight.backend.enums.UploadStatus;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.UploadService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UploadServiceImpl implements UploadService {


private static final long MAX_FILE_SIZE =
        10 * 1024 * 1024;

private final UploadHistoryRepository uploadHistoryRepository;

private final TransactionRepository transactionRepository;

private final FinancialProfileRepository financialProfileRepository;

private final UserRepository userRepository;


// ============================================================
// UPLOAD TRANSACTIONS
// ============================================================

@Override
public UploadResponse uploadTransactions(
        MultipartFile file) {

    validateFile(file);

    User user = getAuthenticatedUser();

    FinancialProfile financialProfile =
            getFinancialProfile(user);

    List<CsvTransactionRow> rows;

    try {

        rows = parseCsv(file);

    } catch (IOException e) {

        throw new IllegalArgumentException(
                "Unable to read the uploaded CSV file.",
                e
        );
    }


    // --------------------------------------------------------
    // Validate CSV
    // --------------------------------------------------------

    validateRows(rows);

    validateMinimumThreeMonths(rows);


    // --------------------------------------------------------
    // Create upload history
    // --------------------------------------------------------

    UploadHistory uploadHistory =
            new UploadHistory();

    uploadHistory.setFinancialProfile(
            financialProfile
    );

    uploadHistory.setOriginalFileName(
            Objects.requireNonNull(
                    file.getOriginalFilename()
            )
    );

    uploadHistory.setTotalRecords(
            rows.size()
    );

    uploadHistory.setImportedRecords(0);

    uploadHistory.setStatus(
            UploadStatus.PROCESSING
    );

    uploadHistory.setUploadedAt(
            LocalDateTime.now()
    );


    UploadHistory savedUpload =
            uploadHistoryRepository.save(
                    uploadHistory
            );


    // --------------------------------------------------------
    // Import transactions
    // --------------------------------------------------------

    int importedRecords = 0;

    try {

        for (CsvTransactionRow row : rows) {

            Transaction transaction =
                    new Transaction();


            transaction.setUser(user);

            transaction.setFinancialProfile(
                    financialProfile
            );

            transaction.setUploadHistory(
                    savedUpload
            );


            // CSV contains LocalDate.
            // Transaction entity uses LocalDateTime.
            transaction.setTransactionDate(
                    row.date().atStartOfDay()
            );


            transaction.setDescription(
                    row.description()
            );


            /*
             * Transaction stores positive monetary values.
             *
             * TransactionType determines whether the
             * transaction is income or expense.
             */
            transaction.setAmount(
                    row.amount().abs()
            );


            transaction.setType(
                    row.type()
            );


            transaction.setCategory(
                    row.category()
            );


            transaction.setPaymentMethod(
                    row.paymentMethod()
            );


            transactionRepository.save(
                    transaction
            );


            importedRecords++;
        }


        // ----------------------------------------------------
        // Upload completed
        // ----------------------------------------------------

        savedUpload.setImportedRecords(
                importedRecords
        );

        savedUpload.setStatus(
                UploadStatus.COMPLETED
        );


        UploadHistory completedUpload =
                uploadHistoryRepository.save(
                        savedUpload
                );


        return mapUploadResponse(
                completedUpload
        );


    } catch (RuntimeException e) {

        savedUpload.setImportedRecords(
                importedRecords
        );

        savedUpload.setStatus(
                UploadStatus.FAILED
        );

        uploadHistoryRepository.save(
                savedUpload
        );

        throw e;
    }
}


// ============================================================
// GET MY UPLOADS
// ============================================================

@Override
@Transactional(readOnly = true)
public List<UploadResponse> getMyUploads() {

    User user =
            getAuthenticatedUser();

    FinancialProfile financialProfile =
            getFinancialProfile(user);


    List<UploadHistory> uploads =
            uploadHistoryRepository
                    .findByFinancialProfileOrderByUploadedAtDesc(
                            financialProfile
                    );


    List<UploadResponse> responses =
            new ArrayList<>();


    for (UploadHistory upload : uploads) {

        responses.add(
                mapUploadResponse(upload)
        );
    }


    return responses;
}


// ============================================================
// GET ONE UPLOAD
// ============================================================

@Override
@Transactional(readOnly = true)
public UploadResponse getUploadById(
        Long uploadId) {

    User user =
            getAuthenticatedUser();

    FinancialProfile financialProfile =
            getFinancialProfile(user);


    UploadHistory uploadHistory =
            uploadHistoryRepository
                    .findByIdAndFinancialProfile(
                            uploadId,
                            financialProfile
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Upload history not found."
                            )
                    );


    return mapUploadResponse(
            uploadHistory
    );
}


// ============================================================
// GET TRANSACTIONS FROM UPLOAD
// ============================================================

@Override
@Transactional(readOnly = true)
public List<TransactionResponse>
        getTransactionsByUpload(
                Long uploadId) {

    User user =
            getAuthenticatedUser();

    FinancialProfile financialProfile =
            getFinancialProfile(user);


    UploadHistory uploadHistory =
            uploadHistoryRepository
                    .findByIdAndFinancialProfile(
                            uploadId,
                            financialProfile
                    )
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Upload history not found."
                            )
                    );


    List<Transaction> transactions =
            transactionRepository
                    .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                            uploadHistory,
                            user
                    );


    List<TransactionResponse> responses =
            new ArrayList<>();


    for (Transaction transaction : transactions) {

        responses.add(
                mapTransactionResponse(
                        transaction
                )
        );
    }


    return responses;
}


// ============================================================
// VALIDATE FILE
// ============================================================

private void validateFile(
        MultipartFile file) {

    if (file == null
            || file.isEmpty()) {

        throw new IllegalArgumentException(
                "Please upload a CSV file."
        );
    }


    if (file.getSize() > MAX_FILE_SIZE) {

        throw new IllegalArgumentException(
                "File size must not exceed 10 MB."
        );
    }


    String fileName =
            file.getOriginalFilename();


    if (fileName == null
            || !fileName
                    .toLowerCase(Locale.ROOT)
                    .endsWith(".csv")) {

        throw new IllegalArgumentException(
                "Only CSV files are supported."
        );
    }
}


// ============================================================
// PARSE CSV
// ============================================================

private List<CsvTransactionRow> parseCsv(
        MultipartFile file)
        throws IOException {

    try (
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    file.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    )
    ) {

        String headerLine =
                reader.readLine();


        if (headerLine == null
                || headerLine.isBlank()) {

            throw new IllegalArgumentException(
                    "The CSV file is empty."
            );
        }


        // ----------------------------------------------------
        // Normalize headers
        // ----------------------------------------------------

        List<String> rawHeaders =
                parseCsvLine(headerLine);

        List<String> headers =
                new ArrayList<>();


        for (String header : rawHeaders) {

            headers.add(
                    normalizeHeader(header)
            );
        }


        // ----------------------------------------------------
        // Create header index map
        // ----------------------------------------------------

        Map<String, Integer> headerIndexes =
                new HashMap<>();


        for (int i = 0;
                i < headers.size();
                i++) {

            headerIndexes.put(
                    headers.get(i),
                    i
            );
        }


        // ----------------------------------------------------
        // Required columns
        // ----------------------------------------------------

        Integer dateIndex =
                findColumn(
                        headerIndexes,
                        "date",
                        "transaction_date",
                        "transactiondate"
                );


        Integer descriptionIndex =
                findColumn(
                        headerIndexes,
                        "description",
                        "transaction_description",
                        "transactiondescription",
                        "details",
                        "merchant",
                        "name"
                );


        Integer amountIndex =
                findColumn(
                        headerIndexes,
                        "amount",
                        "transaction_amount",
                        "transactionamount",
                        "value"
                );


        if (dateIndex == null
                || descriptionIndex == null
                || amountIndex == null) {

            throw new IllegalArgumentException(
                    "CSV must contain Date, Description and Amount columns."
            );
        }


        // ----------------------------------------------------
        // Optional columns
        // ----------------------------------------------------

        Integer typeIndex =
                findColumn(
                        headerIndexes,
                        "type",
                        "transaction_type",
                        "transactiontype"
                );


        Integer categoryIndex =
                findColumn(
                        headerIndexes,
                        "category",
                        "transaction_category",
                        "transactioncategory"
                );


        Integer paymentMethodIndex =
                findColumn(
                        headerIndexes,
                        "payment_method",
                        "paymentmethod",
                        "method"
                );


        List<CsvTransactionRow> rows =
                new ArrayList<>();


        String line;

        int lineNumber = 1;


        // ----------------------------------------------------
        // Read CSV rows
        // ----------------------------------------------------

        while ((line = reader.readLine()) != null) {

            lineNumber++;


            if (line.isBlank()) {
                continue;
            }


            List<String> values =
                    parseCsvLine(line);


            if (allValuesBlank(values)) {
                continue;
            }


            String dateValue =
                    getValue(
                            values,
                            dateIndex
                    );


            String descriptionValue =
                    getValue(
                            values,
                            descriptionIndex
                    );


            String amountValue =
                    getValue(
                            values,
                            amountIndex
                    );


            try {

                LocalDate date =
                        parseDate(
                                dateValue
                        );


                BigDecimal amount =
                        parseAmount(
                                amountValue
                        );


                if (descriptionValue.isBlank()) {

                    throw new IllegalArgumentException(
                            "Description cannot be empty."
                    );
                }


                String typeValue =
                        null;

                if (typeIndex != null) {

                    typeValue =
                            getValue(
                                    values,
                                    typeIndex
                            );
                }


                TransactionType type =
                        parseTransactionType(
                                typeValue,
                                amount
                        );


                String categoryValue =
                        null;

                if (categoryIndex != null) {

                    categoryValue =
                            getValue(
                                    values,
                                    categoryIndex
                            );
                }


                TransactionCategory category =
                        parseCategory(
                                categoryValue
                        );


                String paymentMethodValue =
                        null;

                if (paymentMethodIndex != null) {

                    paymentMethodValue =
                            getValue(
                                    values,
                                    paymentMethodIndex
                            );
                }


                PaymentMethod paymentMethod =
                        parsePaymentMethod(
                                paymentMethodValue
                        );


                CsvTransactionRow row =
                        new CsvTransactionRow(
                                date,
                                descriptionValue.trim(),
                                amount,
                                type,
                                category,
                                paymentMethod
                        );


                rows.add(row);


            } catch (RuntimeException e) {

                String message =
                        e.getMessage();


                if (message == null
                        || message.isBlank()) {

                    message =
                            "Invalid transaction data.";
                }


                throw new IllegalArgumentException(
                        "Invalid data on CSV line "
                                + lineNumber
                                + ": "
                                + message,
                        e
                );
            }
        }


        return rows;
    }
}


// ============================================================
// CHECK WHETHER ALL CSV VALUES ARE BLANK
// ============================================================

private boolean allValuesBlank(
        List<String> values) {

    if (values == null
            || values.isEmpty()) {

        return true;
    }


    for (String value : values) {

        if (value != null
                && !value.isBlank()) {

            return false;
        }
    }


    return true;
}


// ============================================================
// VALIDATE ROWS
// ============================================================

private void validateRows(
        List<CsvTransactionRow> rows) {

    if (rows == null
            || rows.isEmpty()) {

        throw new IllegalArgumentException(
                "The CSV file does not contain any transaction records."
        );
    }
}


// ============================================================
// REQUIRE MINIMUM 3 MONTHS
// ============================================================

private void validateMinimumThreeMonths(
        List<CsvTransactionRow> rows) {

    Set<String> months =
            new HashSet<>();


    LocalDate minDate = null;

    LocalDate maxDate = null;


    for (CsvTransactionRow row : rows) {

        LocalDate date =
                row.date();


        String monthKey =
                date.getYear()
                        + "-"
                        + String.format(
                                Locale.ROOT,
                                "%02d",
                                date.getMonthValue()
                        );


        months.add(monthKey);


        if (minDate == null
                || date.isBefore(minDate)) {

            minDate = date;
        }


        if (maxDate == null
                || date.isAfter(maxDate)) {

            maxDate = date;
        }
    }


    if (months.size() < 3) {

        throw new IllegalArgumentException(
                "At least 3 months of transaction data are required."
        );
    }


    if (minDate == null
            || maxDate == null) {

        throw new IllegalArgumentException(
                "Unable to determine transaction date range."
        );
    }


    long monthDifference =
            ChronoUnit.MONTHS.between(
                    minDate.withDayOfMonth(1),
                    maxDate.withDayOfMonth(1)
            );


    if (monthDifference < 2) {

        throw new IllegalArgumentException(
                "The uploaded transactions must cover at least 3 calendar months."
        );
    }
}


// ============================================================
// PARSE DATE
// ============================================================

private LocalDate parseDate(
        String value) {

    if (value == null
            || value.isBlank()) {

        throw new IllegalArgumentException(
                "Transaction date is required."
        );
    }


    String cleaned =
            value.trim();


    List<DateTimeFormatter> formatters =
            Arrays.asList(
                    DateTimeFormatter.ISO_LOCAL_DATE,

                    DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy"
                    ),

                    DateTimeFormatter.ofPattern(
                            "MM/dd/yyyy"
                    ),

                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy"
                    ),

                    DateTimeFormatter.ofPattern(
                            "MM-dd-yyyy"
                    ),

                    DateTimeFormatter.ofPattern(
                            "yyyy/MM/dd"
                    )
            );


    for (DateTimeFormatter formatter :
            formatters) {

        try {

            return LocalDate.parse(
                    cleaned,
                    formatter
            );

        } catch (DateTimeParseException ignored) {

            // Try next format.
        }
    }


    throw new IllegalArgumentException(
            "Unsupported date format: "
                    + cleaned
    );
}


// ============================================================
// PARSE AMOUNT
// ============================================================

private BigDecimal parseAmount(
        String value) {

    if (value == null
            || value.isBlank()) {

        throw new IllegalArgumentException(
                "Amount is required."
        );
    }


    String cleaned =
            value
                    .trim()
                    .replace(",", "")
                    .replace("$", "")
                    .replace("RM", "")
                    .replace("rm", "")
                    .trim();


    try {

        return new BigDecimal(
                cleaned
        );

    } catch (NumberFormatException e) {

        throw new IllegalArgumentException(
                "Invalid amount: "
                        + value,
                e
        );
    }
}


// ============================================================
// PARSE TRANSACTION TYPE
// ============================================================

private TransactionType parseTransactionType(
        String value,
        BigDecimal amount) {

    if (value == null
            || value.isBlank()) {

        return amount.signum() < 0
                ? TransactionType.EXPENSE
                : TransactionType.INCOME;
    }


    String normalized =
            normalizeEnumValue(
                    value
            );


    if (normalized.equals("INCOME")
            || normalized.equals("CREDIT")
            || normalized.equals("DEPOSIT")) {

        return TransactionType.INCOME;
    }


    if (normalized.equals("EXPENSE")
            || normalized.equals("DEBIT")
            || normalized.equals("WITHDRAWAL")) {

        return TransactionType.EXPENSE;
    }


    throw new IllegalArgumentException(
            "Invalid transaction type: "
                    + value
    );
}


// ============================================================
// PARSE CATEGORY
// ============================================================

private TransactionCategory parseCategory(
        String value) {

    if (value == null
            || value.isBlank()) {

        return TransactionCategory.OTHER;
    }


    String normalized =
            normalizeEnumValue(
                    value
            );


    for (TransactionCategory category :
            TransactionCategory.values()) {

        if (category.name()
                .equals(normalized)) {

            return category;
        }
    }


    Map<String, TransactionCategory> aliases =
            Map.ofEntries(

                    Map.entry(
                            "FOOD_AND_DINING",
                            TransactionCategory.FOOD
                    ),

                    Map.entry(
                            "RESTAURANT",
                            TransactionCategory.FOOD
                    ),

                    Map.entry(
                            "GROCERY",
                            TransactionCategory.GROCERIES
                    ),

                    Map.entry(
                            "TRANSPORT",
                            TransactionCategory.TRANSPORTATION
                    ),

                    Map.entry(
                            "RENT",
                            TransactionCategory.HOUSING
                    ),

                    Map.entry(
                            "BILLS",
                            TransactionCategory.UTILITIES
                    ),

                    Map.entry(
                            "HEALTH",
                            TransactionCategory.HEALTHCARE
                    ),

                    Map.entry(
                            "FUN",
                            TransactionCategory.ENTERTAINMENT
                    ),

                    Map.entry(
                            "ONLINE_SHOPPING",
                            TransactionCategory.SHOPPING
                    ),

                    Map.entry(
                            "PERSONAL",
                            TransactionCategory.PERSONAL_CARE
                    ),

                    Map.entry(
                            "SUBSCRIPTIONS",
                            TransactionCategory.SUBSCRIPTION
                    ),

                    Map.entry(
                            "INVESTMENTS",
                            TransactionCategory.INVESTMENT
                    )
            );


    TransactionCategory alias =
            aliases.get(normalized);


    if (alias != null) {
        return alias;
    }


    return TransactionCategory.OTHER;
}


// ============================================================
// PARSE PAYMENT METHOD
// ============================================================

private PaymentMethod parsePaymentMethod(
        String value) {

    if (value == null
            || value.isBlank()) {

        return PaymentMethod.OTHER;
    }


    String normalized =
            normalizeEnumValue(
                    value
            );


    for (PaymentMethod method :
            PaymentMethod.values()) {

        if (method.name()
                .equals(normalized)) {

            return method;
        }
    }


    Map<String, PaymentMethod> aliases =
            Map.ofEntries(

                    Map.entry(
                            "DEBIT",
                            PaymentMethod.DEBIT_CARD
                    ),

                    Map.entry(
                            "DEBITCARD",
                            PaymentMethod.DEBIT_CARD
                    ),

                    Map.entry(
                            "CREDIT",
                            PaymentMethod.CREDIT_CARD
                    ),

                    Map.entry(
                            "CREDITCARD",
                            PaymentMethod.CREDIT_CARD
                    ),

                    Map.entry(
                            "BANK",
                            PaymentMethod.BANK_TRANSFER
                    ),

                    Map.entry(
                            "BANKTRANSFER",
                            PaymentMethod.BANK_TRANSFER
                    ),

                    Map.entry(
                            "EWALLET",
                            PaymentMethod.E_WALLET
                    ),

                    Map.entry(
                            "TOUCH_N_GO",
                            PaymentMethod.E_WALLET
                    ),

                    Map.entry(
                            "TNG",
                            PaymentMethod.E_WALLET
                    )
            );


    PaymentMethod alias =
            aliases.get(normalized);


    if (alias != null) {
        return alias;
    }


    return PaymentMethod.OTHER;
}


// ============================================================
// NORMALIZE ENUM VALUE
// ============================================================

private String normalizeEnumValue(
        String value) {

    if (value == null) {
        return "";
    }


    return value
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace("&", "AND")
            .replace("-", "_")
            .replace(" ", "_")
            .replace("/", "_");
}


// ============================================================
// NORMALIZE CSV HEADER
// ============================================================

private String normalizeHeader(
        String header) {

    if (header == null) {
        return "";
    }


    return header
            .replace("\uFEFF", "")
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("-", "_")
            .replace(" ", "_");
}


// ============================================================
// FIND COLUMN
// ============================================================

private Integer findColumn(
        Map<String, Integer> indexes,
        String... names) {

    for (String name : names) {

        String normalized =
                normalizeHeader(name);


        Integer index =
                indexes.get(normalized);


        if (index != null) {
            return index;
        }
    }


    return null;
}


// ============================================================
// GET CSV VALUE
// ============================================================

private String getValue(
        List<String> values,
        Integer index) {

    if (index == null
            || index < 0
            || index >= values.size()) {

        return "";
    }


    String value =
            values.get(index);


    if (value == null) {
        return "";
    }


    return value.trim();
}


// ============================================================
// CSV LINE PARSER
// ============================================================

private List<String> parseCsvLine(
        String line) {

    List<String> values =
            new ArrayList<>();


    StringBuilder current =
            new StringBuilder();


    boolean insideQuotes =
            false;


    for (int i = 0;
            i < line.length();
            i++) {

        char character =
                line.charAt(i);


        if (character == '"') {

            if (insideQuotes
                    && i + 1 < line.length()
                    && line.charAt(i + 1) == '"') {

                current.append('"');

                i++;

            } else {

                insideQuotes =
                        !insideQuotes;
            }


        } else if (
                character == ','
                        && !insideQuotes) {

            values.add(
                    current.toString()
            );

            current.setLength(0);


        } else {

            current.append(
                    character
            );
        }
    }


    if (insideQuotes) {

        throw new IllegalArgumentException(
                "CSV contains an unclosed quoted value."
        );
    }


    values.add(
            current.toString()
    );


    return values;
}


// ============================================================
// MAP UPLOAD RESPONSE
// ============================================================

private UploadResponse mapUploadResponse(
        UploadHistory uploadHistory) {

    return new UploadResponse(

            uploadHistory.getId(),

            uploadHistory
                    .getOriginalFileName(),

            uploadHistory
                    .getTotalRecords(),

            uploadHistory
                    .getImportedRecords(),

            uploadHistory
                    .getStatus()
                    .name(),

            uploadHistory
                    .getUploadedAt()
    );
}


// ============================================================
// MAP TRANSACTION RESPONSE
// ============================================================

private TransactionResponse
        mapTransactionResponse(
                Transaction transaction) {

    return new TransactionResponse(

            transaction.getId(),

            transaction
                    .getTransactionDate(),

            transaction
                    .getDescription(),

            transaction
                    .getAmount(),

            transaction
                    .getType(),

            transaction
                    .getCategory(),

            transaction
                    .getPaymentMethod(),

            transaction
                    .getCreatedAt(),

            transaction
                    .getUpdatedAt()
    );
}


// ============================================================
// GET AUTHENTICATED USER
// ============================================================

private User getAuthenticatedUser() {

    Authentication authentication =
            SecurityContextHolder
                    .getContext()
                    .getAuthentication();


    if (authentication == null
            || !authentication.isAuthenticated()
            || authentication.getName() == null
            || "anonymousUser".equals(
                    authentication.getName()
            )) {

        throw new RuntimeException(
                "User is not authenticated."
        );
    }


    String email =
            authentication.getName();


    return userRepository
            .findByEmail(email)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Authenticated user not found."
                    )
            );
}


// ============================================================
// GET FINANCIAL PROFILE
// ============================================================

private FinancialProfile getFinancialProfile(
        User user) {

    return financialProfileRepository
            .findByUser(user)
            .orElseThrow(() ->
                    new RuntimeException(
                            "Financial profile not found. "
                                    + "Please create your financial profile "
                                    + "before uploading transactions."
                    )
            );
}


// ============================================================
// CSV ROW RECORD
// ============================================================

private record CsvTransactionRow(

        LocalDate date,

        String description,

        BigDecimal amount,

        TransactionType type,

        TransactionCategory category,

        PaymentMethod paymentMethod

) {
}


}
