package com.example.spliteasyweb.service;

import com.example.spliteasyweb.model.ExpenseEntity;
import com.example.spliteasyweb.model.SettlementEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.math.BigDecimal;

@Service
public class SettlementService {

  public Map<String, Double> balances(List<ExpenseEntity> expenses){
    Map<String, Double> bal = new HashMap<>();

    for (var e: expenses){
      List<String> parts  = parse(e.getParticipantsCsv());
      List<String> payers = parse(e.getPayersCsv()); // puede venir null -> parse lo maneja
      int nParts  = parts.size();

      if (nParts == 0) continue;

      double amount = (e.getAmount() == null ? 0.0 : e.getAmount().doubleValue());
      if (amount == 0.0) continue;

      // Fallback: si no hay pagadores explícitos, usar el primer participante
      if (payers.isEmpty() && !parts.isEmpty()) {
        payers = List.of(parts.get(0));
      }

      int nPayers = payers.size(); // ya nunca es 0 por el fallback

      double sharePerPart  = amount / nParts;
      double sharePerPayer = amount / nPayers;

      // crédito a cada pagador
      for (String payer: payers) {
        bal.merge(payer, sharePerPayer, Double::sum);
      }

      // Debit each participant using explicit allocations when present.
      Map<String, Double> allocations = parseAllocations(e.getSplitAllocations());
      for (String p: parts) {
        double share = allocations.getOrDefault(p, sharePerPart);
        bal.merge(p, -share, Double::sum);
      }
    }

    // redondeo a 2 decimales
    bal.replaceAll((k,v)-> Math.round(v*100.0)/100.0);
    return new TreeMap<>(bal);
  }

  public record Transfer(String from, String to, double amount){}
  private record Entry(String person, double amount){}

  public List<Transfer> settle(Map<String, Double> bal){
    PriorityQueue<Entry> debt = new PriorityQueue<>(Comparator.comparingDouble(e->e.amount));
    PriorityQueue<Entry> cred = new PriorityQueue<>((a,b)->Double.compare(b.amount,a.amount));

    for (var e: bal.entrySet()){
      double v=e.getValue();
      if (v<-0.01) debt.add(new Entry(e.getKey(), -v));
      else if (v>0.01) cred.add(new Entry(e.getKey(), v));
    }

    List<Transfer> out=new ArrayList<>();
    while(!debt.isEmpty() && !cred.isEmpty()){
      var d=debt.poll(); var c=cred.poll();
      double m=Math.min(d.amount,c.amount); m=round(m);
      out.add(new Transfer(d.person(), c.person(), m));
      if (d.amount>m) debt.add(new Entry(d.person(), round(d.amount-m)));
      if (c.amount>m) cred.add(new Entry(c.person(), round(c.amount-m)));
    }
    return out;
  }

  public void applySettlements(Map<String, Double> balances, List<SettlementEntity> settlements) {
    for (SettlementEntity settlement : settlements) {
      double amount = settlement.getAmount() == null ? 0.0 : settlement.getAmount().doubleValue();
      if (amount <= 0.0) continue;
      balances.merge(settlement.getFromPerson(), amount, Double::sum);
      balances.merge(settlement.getToPerson(), -amount, Double::sum);
    }
    balances.replaceAll((key, value) -> Math.round(value * 100.0) / 100.0);
  }

  public static List<String> parse(String csv){
    if (csv==null || csv.isBlank()) return List.of();
    var set = new LinkedHashSet<String>();
    for (var s: csv.split(",")){
      var t=s.trim();
      if(!t.isEmpty()) set.add(t);
    }
    return new ArrayList<>(set);
  }

  public static String join(List<String> list){
    return String.join(",", list);
  }

  private static Map<String, Double> parseAllocations(String encoded) {
    if (encoded == null || encoded.isBlank()) return Map.of();
    Map<String, Double> allocations = new HashMap<>();
    for (String item : encoded.split(",")) {
      String[] pair = item.split("=", 2);
      if (pair.length != 2) continue;
      try {
        allocations.put(pair[0], new BigDecimal(pair[1]).doubleValue());
      } catch (NumberFormatException ignored) {
        // Invalid legacy allocation falls back to equal splitting.
      }
    }
    return allocations;
  }

  private double round(double x){ return Math.round(x*100.0)/100.0; }
}
