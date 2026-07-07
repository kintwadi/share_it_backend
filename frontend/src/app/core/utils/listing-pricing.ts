import { Listing, ListingPricingUnit, ListingType } from '../models/types';

export function getListingPricingUnit(listing: Listing | null | undefined): ListingPricingUnit {
  const raw = String((listing as any)?.pricingUnit || '').trim().toUpperCase();
  if (raw === ListingPricingUnit.DAILY) return ListingPricingUnit.DAILY;
  if (raw === ListingPricingUnit.MONTHLY) return ListingPricingUnit.MONTHLY;
  return ListingPricingUnit.HOURLY;
}

export function getListingPrimaryRate(listing: Listing | null | undefined): number {
  if (!listing) return 0;
  if (listing.type === ListingType.GIVE || listing.type === ListingType.SELL) {
    return getNumericRate((listing as any)?.hourlyRate);
  }
  return getListingRateForUnit(listing, getListingPricingUnit(listing));
}

export function isListingFree(listing: Listing | null | undefined): boolean {
  if (!listing) return false;
  if (listing.type === ListingType.GIVE) return true;
  return getListingPrimaryRate(listing) <= 0;
}

export function getPricingUnitShort(unit: ListingPricingUnit): string {
  if (unit === ListingPricingUnit.DAILY) return '/day';
  if (unit === ListingPricingUnit.MONTHLY) return '/mo';
  return '/hr';
}

export function getPricingUnitLong(unit: ListingPricingUnit): string {
  if (unit === ListingPricingUnit.DAILY) return 'day';
  if (unit === ListingPricingUnit.MONTHLY) return 'month';
  return 'hour';
}

export function getPricingUnitPlural(unit: ListingPricingUnit): string {
  if (unit === ListingPricingUnit.DAILY) return 'days';
  if (unit === ListingPricingUnit.MONTHLY) return 'months';
  return 'hours';
}

export function getListingPriceSuffix(listing: Listing | null | undefined): string {
  if (!listing || listing.type === ListingType.GIVE || listing.type === ListingType.SELL) return '';
  return getPricingUnitShort(getListingPricingUnit(listing));
}

export function getListingRateForUnit(listing: Listing | null | undefined, unit: ListingPricingUnit): number {
  if (!listing) return 0;
  if (listing.type === ListingType.GIVE) return 0;
  if (listing.type === ListingType.SELL) return getNumericRate((listing as any)?.hourlyRate);

  const hourly = getNumericRate((listing as any)?.hourlyRate);
  const daily = getNumericRate((listing as any)?.dailyRate);
  const monthly = getNumericRate((listing as any)?.monthlyRate);
  const primaryUnit = getListingPricingUnit(listing);

  if (unit === ListingPricingUnit.DAILY) {
    if (daily > 0) return daily;
    if (primaryUnit === ListingPricingUnit.DAILY && hourly > 0) return hourly;
    return 0;
  }
  if (unit === ListingPricingUnit.MONTHLY) {
    if (monthly > 0) return monthly;
    if (primaryUnit === ListingPricingUnit.MONTHLY && hourly > 0) return hourly;
    return 0;
  }
  return hourly;
}

export function hasListingRateForUnit(listing: Listing | null | undefined, unit: ListingPricingUnit): boolean {
  return getListingRateForUnit(listing, unit) > 0;
}

export function getListingAdditionalRates(listing: Listing | null | undefined): Array<{ unit: ListingPricingUnit; rate: number }> {
  if (!listing || listing.type !== ListingType.LEND) return [];
  const primary = getListingPricingUnit(listing);
  return [ListingPricingUnit.HOURLY, ListingPricingUnit.DAILY, ListingPricingUnit.MONTHLY]
    .filter(unit => unit !== primary)
    .map(unit => ({ unit, rate: getListingRateForUnit(listing, unit) }))
    .filter(item => item.rate > 0);
}

function getNumericRate(value: unknown): number {
  const rate = Number(value ?? 0);
  return Number.isFinite(rate) && rate > 0 ? rate : 0;
}
