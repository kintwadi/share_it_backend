import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  BikeAdminSkuRequest,
  BikeAdminSpecSelectionRequest,
  BikeAdminUpsertBikeRequest,
  BikeShopDetail
} from '../../models/bike';
import { BikeApiService } from '../../services/bike-api.service';

type BikeAdminSpecDraft = {
  attributeName: string;
  valuesText: string;
};

type BikeAdminSkuDraft = BikeAdminSkuRequest;

@Component({
  selector: 'app-bike-catalog-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './bike-catalog-admin.component.html',
  styleUrl: './bike-catalog-admin.component.css'
})
export class BikeCatalogAdminComponent implements OnInit {
  private readonly bikeApi = inject(BikeApiService);
  private readonly route = inject(ActivatedRoute);

  readonly categoryOptions = [
    { value: 'ROAD', label: 'Road' },
    { value: 'GRAVEL', label: 'Gravel' },
    { value: 'MTB', label: 'Mountain Bike' },
    { value: 'E_BIKE', label: 'E-Bike' }
  ];

  readonly saleTypeOptions = [
    { value: 'COMPLETE_BIKE', label: 'Complete Bike' },
    { value: 'FRAMESET', label: 'Frameset' }
  ];

  form: BikeAdminUpsertBikeRequest = {
    brandName: '',
    modelName: '',
    modelYear: new Date().getFullYear(),
    category: 'ROAD',
    saleType: 'COMPLETE_BIKE',
    basePrice: 0,
    description: '',
    imageUrl: '',
    isActive: true,
    specs: [],
    skus: []
  };

  specDrafts: BikeAdminSpecDraft[] = [
    { attributeName: 'Frame Material', valuesText: 'Carbon' }
  ];

  skuDrafts: BikeAdminSkuDraft[] = [
    this.createSkuDraft()
  ];

  loading = false;
  loadingExisting = false;
  error = '';
  success = '';
  savedBike: BikeShopDetail | null = null;
  editBikeId: number | null = null;

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }

    this.loadingExisting = true;
    this.error = '';

    try {
      const bike = await this.bikeApi.getShopBike(id);
      this.editBikeId = bike.id;
      this.savedBike = bike;
      this.hydrateFromBike(bike);
    } catch {
      this.error = 'Unable to load the existing catalog bike.';
    } finally {
      this.loadingExisting = false;
    }
  }

  get previewImageUrl(): string {
    return this.form.imageUrl?.trim() || 'https://placehold.co/1200x800/f5f5f5/7a7a7a/png?text=Bike+Image+Preview';
  }

  get isEditMode(): boolean {
    return this.editBikeId !== null;
  }

  addSpec(): void {
    this.specDrafts = [...this.specDrafts, { attributeName: '', valuesText: '' }];
  }

  removeSpec(index: number): void {
    this.specDrafts = this.specDrafts.filter((_, currentIndex) => currentIndex !== index);
  }

  addSku(): void {
    this.skuDrafts = [...this.skuDrafts, this.createSkuDraft()];
  }

  removeSku(index: number): void {
    if (this.skuDrafts.length === 1) {
      this.skuDrafts = [this.createSkuDraft()];
      return;
    }
    this.skuDrafts = this.skuDrafts.filter((_, currentIndex) => currentIndex !== index);
  }

  async submit(): Promise<void> {
    this.loading = true;
    this.error = '';
    this.success = '';

    const payload: BikeAdminUpsertBikeRequest = {
      ...this.form,
      brandName: this.form.brandName.trim(),
      modelName: this.form.modelName.trim(),
      description: this.form.description?.trim() || null,
      imageUrl: this.form.imageUrl?.trim() || null,
      specs: this.buildSpecsPayload(),
      skus: this.buildSkuPayload()
    };

    if (!payload.brandName || !payload.modelName) {
      this.error = 'Brand and model are required.';
      this.loading = false;
      return;
    }

    if (!payload.skus.length) {
      this.error = 'At least one SKU is required.';
      this.loading = false;
      return;
    }

    try {
      this.savedBike = await this.bikeApi.upsertShopBike(payload);
      this.editBikeId = this.savedBike.id;
      this.success = 'Catalog bike saved successfully.';
    } catch {
      this.savedBike = null;
      this.error = 'Catalog save failed. Make sure this account can access the bike admin endpoint.';
    } finally {
      this.loading = false;
    }
  }

  private buildSpecsPayload(): BikeAdminSpecSelectionRequest[] {
    return this.specDrafts
      .map((spec) => ({
        attributeName: spec.attributeName.trim(),
        values: spec.valuesText
          .split(/\r?\n|,/)
          .map((value) => value.trim())
          .filter(Boolean)
      }))
      .filter((spec) => spec.attributeName && spec.values.length);
  }

  private buildSkuPayload(): BikeAdminSkuRequest[] {
    return this.skuDrafts
      .map((sku) => ({
        ...sku,
        skuCode: sku.skuCode.trim(),
        colorName: sku.colorName.trim(),
        sizeValue: sku.sizeValue.trim(),
        priceModifier: sku.priceModifier ?? 0
      }))
      .filter((sku) => sku.skuCode && sku.colorName && sku.sizeValue);
  }

  private createSkuDraft(): BikeAdminSkuDraft {
    return {
      id: null,
      skuCode: '',
      colorName: '',
      sizeValue: '',
      riderHeightMinCm: null,
      riderHeightMaxCm: null,
      stackMm: null,
      reachMm: null,
      stockQuantity: 0,
      priceModifier: 0
    };
  }

  private hydrateFromBike(bike: BikeShopDetail): void {
    this.form = {
      brandName: bike.brandName,
      modelName: bike.modelName,
      modelYear: bike.modelYear,
      category: bike.category,
      saleType: bike.saleType,
      basePrice: bike.basePrice,
      description: bike.description,
      imageUrl: bike.imageUrl,
      isActive: bike.active,
      specs: [],
      skus: []
    };

    this.specDrafts = bike.specs.length
      ? bike.specs.map((spec) => ({
          attributeName: spec.attributeName,
          valuesText: spec.values.join(', ')
        }))
      : [{ attributeName: '', valuesText: '' }];

    this.skuDrafts = bike.skus.length
      ? bike.skus.map((sku) => ({
          id: sku.id,
          skuCode: sku.skuCode,
          colorName: sku.colorName,
          sizeValue: sku.sizeValue,
          riderHeightMinCm: sku.riderHeightMinCm,
          riderHeightMaxCm: sku.riderHeightMaxCm,
          stackMm: sku.stackMm,
          reachMm: sku.reachMm,
          stockQuantity: sku.stockQuantity,
          priceModifier: sku.priceModifier ?? 0
        }))
      : [this.createSkuDraft()];
  }
}
