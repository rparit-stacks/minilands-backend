package com.minilands.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One accrual-window monthly payment distribution (separate from calendar {@link RoiDistribution} rows).
 */
@Document(collection = "monthly_payment_runs")
public class MonthlyPaymentRun {

    @Id
    private String id;

    @Indexed
    private String propertyId;

    private Instant accrualStart;

    @Indexed
    private Instant accrualEnd;

    private BigDecimal monthlyAmountConfigured;
    private Integer platformFeePercent;

    private BigDecimal poolGross;
    private BigDecimal platformFeeAmount;
    private BigDecimal poolNet;

    /** totalShares × elapsedDays — full cap-table share-days for the accrual window. */
    private BigDecimal denominatorShareDays;
    /** Sum of (shares × eligibleDays) across active investor holdings in the window. */
    private BigDecimal investorShareDaysSum;
    /** Legacy field: previously investor-only weight sum; prefer {@link #denominatorShareDays}. */
    private BigDecimal totalShareDayWeight;
    private BigDecimal spvDistributed;
    private BigDecimal totalDistributed;
    private int investorsPaid;

    private Instant createdAt;

    public MonthlyPaymentRun() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public Instant getAccrualStart() {
        return accrualStart;
    }

    public void setAccrualStart(Instant accrualStart) {
        this.accrualStart = accrualStart;
    }

    public Instant getAccrualEnd() {
        return accrualEnd;
    }

    public void setAccrualEnd(Instant accrualEnd) {
        this.accrualEnd = accrualEnd;
    }

    public BigDecimal getMonthlyAmountConfigured() {
        return monthlyAmountConfigured;
    }

    public void setMonthlyAmountConfigured(BigDecimal monthlyAmountConfigured) {
        this.monthlyAmountConfigured = monthlyAmountConfigured;
    }

    public Integer getPlatformFeePercent() {
        return platformFeePercent;
    }

    public void setPlatformFeePercent(Integer platformFeePercent) {
        this.platformFeePercent = platformFeePercent;
    }

    public BigDecimal getPoolGross() {
        return poolGross;
    }

    public void setPoolGross(BigDecimal poolGross) {
        this.poolGross = poolGross;
    }

    public BigDecimal getPlatformFeeAmount() {
        return platformFeeAmount;
    }

    public void setPlatformFeeAmount(BigDecimal platformFeeAmount) {
        this.platformFeeAmount = platformFeeAmount;
    }

    public BigDecimal getPoolNet() {
        return poolNet;
    }

    public void setPoolNet(BigDecimal poolNet) {
        this.poolNet = poolNet;
    }

    public BigDecimal getDenominatorShareDays() {
        return denominatorShareDays;
    }

    public void setDenominatorShareDays(BigDecimal denominatorShareDays) {
        this.denominatorShareDays = denominatorShareDays;
    }

    public BigDecimal getInvestorShareDaysSum() {
        return investorShareDaysSum;
    }

    public void setInvestorShareDaysSum(BigDecimal investorShareDaysSum) {
        this.investorShareDaysSum = investorShareDaysSum;
    }

    public BigDecimal getTotalShareDayWeight() {
        return totalShareDayWeight;
    }

    public void setTotalShareDayWeight(BigDecimal totalShareDayWeight) {
        this.totalShareDayWeight = totalShareDayWeight;
    }

    public BigDecimal getSpvDistributed() {
        return spvDistributed;
    }

    public void setSpvDistributed(BigDecimal spvDistributed) {
        this.spvDistributed = spvDistributed;
    }

    public BigDecimal getTotalDistributed() {
        return totalDistributed;
    }

    public void setTotalDistributed(BigDecimal totalDistributed) {
        this.totalDistributed = totalDistributed;
    }

    public int getInvestorsPaid() {
        return investorsPaid;
    }

    public void setInvestorsPaid(int investorsPaid) {
        this.investorsPaid = investorsPaid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
