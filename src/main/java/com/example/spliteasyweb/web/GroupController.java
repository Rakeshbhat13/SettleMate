package com.example.spliteasyweb.web;

import com.example.spliteasyweb.model.ExpenseEntity;
import com.example.spliteasyweb.model.GroupEntity;
import com.example.spliteasyweb.model.PersonEntity;
import com.example.spliteasyweb.model.ExpenseCategory;
import com.example.spliteasyweb.model.SplitType;
import com.example.spliteasyweb.repo.ExpenseRepo;
import com.example.spliteasyweb.repo.GroupRepo;
import com.example.spliteasyweb.repo.PersonRepo;
import com.example.spliteasyweb.repo.SettlementRepo;
import com.example.spliteasyweb.model.SettlementEntity;
import com.example.spliteasyweb.service.SettlementService;
import com.example.spliteasyweb.service.AnalyticsService;
import com.example.spliteasyweb.service.SplitCalculator;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GroupController {

    private final GroupRepo groups;
    private final PersonRepo people;
    private final ExpenseRepo expenses;
    private final SettlementService settlementService;
    private final AnalyticsService analyticsService;
    private final SettlementRepo settlements;
    private final SplitCalculator splitCalculator;

    public GroupController(GroupRepo groups, PersonRepo people, ExpenseRepo expenses,
                           SettlementService settlementService, AnalyticsService analyticsService,
                           SettlementRepo settlements, SplitCalculator splitCalculator) {
        this.groups = groups;
        this.people = people;
        this.expenses = expenses;
        this.settlementService = settlementService;
        this.analyticsService = analyticsService;
        this.settlements = settlements;
        this.splitCalculator = splitCalculator;
    }

    // Crear grupo (desde el home)
    @PostMapping("/groups")
    @Transactional
    public String createGroup(@RequestParam String name,
                              @RequestParam(required = false) BigDecimal budget,
                              HttpSession session) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty())
            return "redirect:/";

        String base = slugify(normalized);
        String slug = base;
        int i = 1;
        while (groups.existsBySlug(slug)) {
            i++;
            slug = base + "-" + i;
        }

        GroupEntity g = new GroupEntity();
        g.setName(normalized);
        g.setSlug(slug);
        if (budget != null && budget.signum() > 0) {
            g.setBudget(budget.setScale(2, RoundingMode.HALF_UP));
        }
        g.setOwnerSession(session.getId());
        groups.save(g);

        // guardar recientes en sesión (opcional)
        @SuppressWarnings("unchecked")
        var recent = (LinkedList<String>) Optional.ofNullable(session.getAttribute("recentGroups"))
                .orElse(new LinkedList<String>());
        recent.remove(slug);
        recent.addFirst(slug);
        while (recent.size() > 5)
            recent.removeLast();
        session.setAttribute("recentGroups", recent);

        return "redirect:/g/" + slug;
    }

    // Dashboard del grupo
    @GetMapping("/g/{slug}")
    public String dashboard(@PathVariable String slug, Model model) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        var ppl = people.findByGroupIdOrderByNameAsc(g.getId());
        var exps = expenses.findByGroupIdOrderByDateDescIdDesc(g.getId());

        var balances = settlementService.balances(exps);
        var history = settlements.findByGroupIdOrderBySettledAtDesc(g.getId());
        settlementService.applySettlements(balances, history);
        var txs = settlementService.settle(balances);
        var summary = analyticsService.summarize(exps, ppl.size(), g.getBudget());

        model.addAttribute("g", g);
        model.addAttribute("people", ppl);
        model.addAttribute("expenses", exps);
        model.addAttribute("balances", balances);
        model.addAttribute("txs", txs);
        model.addAttribute("summary", summary);
        model.addAttribute("settlementHistory", history);
        return "group";
    }

    // Personas
    @PostMapping("/g/{slug}/people")
    @Transactional
    public String addPerson(@PathVariable String slug, @RequestParam String name) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty())
            return "redirect:/g/" + slug;

        var existing = people.findByGroupIdOrderByNameAsc(g.getId());
        boolean exists = existing.stream().anyMatch(p -> norm(p.getName()).equals(norm(trimmed)));
        if (!exists) {
            PersonEntity p = new PersonEntity();
            p.setGroupId(g.getId());
            p.setName(trimmed);
            people.save(p);
        }
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/people/{id}/delete")
    @Transactional
    public String deletePerson(@PathVariable String slug, @PathVariable Long id) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        people.findByIdAndGroupId(id, g.getId()).ifPresent(people::delete);
        return "redirect:/g/" + slug;
    }

    // Gastos
    @PostMapping("/g/{slug}/expenses")
    @Transactional
    public String addExpense(@PathVariable String slug,
                             @RequestParam String title,
                             @RequestParam String amount,
                             // El front manda payers como CSV y participants como múltiples inputs name=participants
                             @RequestParam(name = "payers", required = false) String payersCsvRaw,
                             @RequestParam(name = "participants", required = false) List<String> participantsList,
                             @RequestParam(name = "category", required = false) ExpenseCategory category,
                             @RequestParam(name = "splitType", required = false) SplitType splitType,
                             @RequestParam(name = "allocationValues", required = false) String allocationValues) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();

        var ppl = people.findByGroupIdOrderByNameAsc(g.getId());
        Map<String, String> canon = canonicalMap(ppl);

        String participantsCsv = (participantsList == null) ? "" : String.join(",", participantsList);
        String payersCsv = payersCsvRaw; // puede ser null/empty

        ExpenseEntity e = new ExpenseEntity();
        e.setGroup(g);
        e.setTitle(title == null ? "" : title.trim());
        e.setAmount(safeBigDecimal(amount));
        e.setDate(LocalDate.now());
        e.setCategory(category);
        e.setSplitType(splitType);
        e.setParticipantsCsv(joinUniqueCanonical(participantsCsv, canon));
        e.setPayersCsv(joinUniqueCanonical(payersCsv, canon));

        List<String> canonicalParticipants = SettlementService.parse(e.getParticipantsCsv());
        try {
            Map<String, BigDecimal> allocations = parseAllocationValues(allocationValues, canonicalParticipants);
            Map<String, BigDecimal> calculated = splitCalculator.calculate(
                    e.getAmount(), canonicalParticipants, e.getSplitType(), allocations);
            e.setSplitAllocations(encodeAllocations(calculated));
        } catch (IllegalArgumentException ex) {
            return "redirect:/g/" + slug;
        }

        expenses.save(e);
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/expenses/{id}/delete")
    @Transactional
    public String deleteExpense(@PathVariable String slug, @PathVariable Long id) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        expenses.findByIdAndGroupId(id, g.getId()).ifPresent(expenses::delete);
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/expenses/{id}/liquidate")
    @Transactional
    public String liquidateExpense(@PathVariable String slug, @PathVariable Long id) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        expenses.findByIdAndGroupId(id, g.getId()).ifPresent(expenses::delete);
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/settlements")
    @Transactional
    public String recordSettlement(@PathVariable String slug,
                                   @RequestParam String from,
                                   @RequestParam String to,
                                   @RequestParam BigDecimal amount,
                                   @RequestParam(required = false) String note) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        if (amount.signum() <= 0 || from.isBlank() || to.isBlank() || from.equalsIgnoreCase(to)) {
            return "redirect:/g/" + slug;
        }
        SettlementEntity settlement = new SettlementEntity();
        settlement.setGroupId(g.getId());
        settlement.setFromPerson(from.trim());
        settlement.setToPerson(to.trim());
        settlement.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        settlement.setNote(note == null ? "" : note.trim());
        settlements.save(settlement);
        return "redirect:/g/" + slug;
    }

    // Export CSV (3 endpoints)

    @GetMapping(value = "/g/{slug}/export/expenses.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<String> exportExpensesCsv(@PathVariable String slug) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        var exps = expenses.findByGroupIdOrderByDateDescIdDesc(g.getId());

        StringBuilder sb = new StringBuilder();
        // encabezado
        sb.append("Fecha,Título,Pagó,Participantes,Monto\n");
        for (var e : exps) {
            String date = e.getDate() == null ? "" : e.getDate().toString();
            String title = csv(e.getTitle());
            String payers = csv(csvListPretty(e.getPayersCsv()));
            String participants = csv(csvListPretty(e.getParticipantsCsv()));
            String amount = e.getAmount() == null ? "0.00" : String.format(java.util.Locale.US, "%.2f", e.getAmount());

            sb.append(date).append(",")
              .append("\"").append(title).append("\",")
              .append("\"").append(payers).append("\",")
              .append("\"").append(participants).append("\",")
              .append(amount).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"expenses-" + slug + ".csv\"")
                .body(sb.toString());
    }

    @GetMapping(value = "/g/{slug}/export/balances.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<String> exportBalancesCsv(@PathVariable String slug) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        var exps = expenses.findByGroupIdOrderByDateDescIdDesc(g.getId());
        var balances = settlementService.balances(exps);

        StringBuilder sb = new StringBuilder();
        sb.append("Persona,Balance\n");
        for (var e : balances.entrySet()) {
            String person = csv(e.getKey());
            String val = String.format(java.util.Locale.US, "%.2f", e.getValue());
            sb.append("\"").append(person).append("\",").append(val).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"balances-" + slug + ".csv\"")
                .body(sb.toString());
    }

    @GetMapping(value = "/g/{slug}/export/transfers.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<String> exportTransfersCsv(@PathVariable String slug) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        var exps = expenses.findByGroupIdOrderByDateDescIdDesc(g.getId());
        var balances = settlementService.balances(exps);
        var txs = settlementService.settle(balances);

        StringBuilder sb = new StringBuilder();
        sb.append("De,Para,Monto\n");
        for (var t : txs) {
            String from = csv(t.from());
            String to = csv(t.to());
            String amount = String.format(java.util.Locale.US, "%.2f", t.amount());
            sb.append("\"").append(from).append("\",")
              .append("\"").append(to).append("\",")
              .append(amount).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"transfers-" + slug + ".csv\"")
                .body(sb.toString());
    }

    // Helpers CSV
    private static String csv(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    private static String csvListPretty(String csv) {
        if (csv == null || csv.isBlank()) return "";
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.joining(", "));
    }

    // Helpers
    private static String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return n.isEmpty() ? "grupo" : n;
    }

    private static String norm(String s) {
        if (s == null)
            return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private static Map<String, String> canonicalMap(List<PersonEntity> people) {
        Map<String, String> map = new HashMap<>();
        for (var p : people)
            map.put(norm(p.getName()), p.getName());
        return map;
    }

    private static String joinUniqueCanonical(String csv, Map<String, String> canon) {
        if (csv == null)
            return "";
        var set = new LinkedHashSet<String>();
        for (String token : csv.split(",")) {
            String raw = token.trim();
            if (raw.isEmpty())
                continue;
            String key = norm(raw);
            String canonical = canon.getOrDefault(key, raw);
            if (set.stream().noneMatch(x -> norm(x).equals(norm(canonical))))
                set.add(canonical);
        }
        return String.join(",", set);
    }

    private static BigDecimal safeBigDecimal(String s) {
        try {
            if (s == null)
                return BigDecimal.ZERO;
            s = s.trim().replace(',', '.');
            if (s.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private static Map<String, BigDecimal> parseAllocationValues(String raw, List<String> participants) {
        if (raw == null || raw.isBlank()) return Map.of();
        String[] values = raw.split(",", -1);
        if (values.length != participants.size()) {
            throw new IllegalArgumentException("One allocation is required per participant");
        }
        Map<String, BigDecimal> allocations = new LinkedHashMap<>();
        for (int index = 0; index < participants.size(); index++) {
            try {
                allocations.put(participants.get(index), new BigDecimal(values[index].trim().replace(',', '.')));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid allocation");
            }
        }
        return allocations;
    }

    private static String encodeAllocations(Map<String, BigDecimal> allocations) {
        return allocations.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().toPlainString())
                .collect(Collectors.joining(","));
    }
}
