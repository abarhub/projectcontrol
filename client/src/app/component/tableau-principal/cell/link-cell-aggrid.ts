import {ICellRendererAngularComp} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';
import {LigneTableauPrincipal} from '../ligne-tableau-principal';

@Component({
  selector: 'colour-cell',
  imports: [
    RouterLink
  ],
  template: `@if (params.data.groupeId && params.value) {
    <a [routerLink]="['details',params.data.groupeId, params.data.id]">{{ params.value }}</a>
  } @else {
    {{ params.value }}
  }`
})
export class LinkCellAgGrid implements ICellRendererAngularComp {

  params!: ICellRendererParams;
  data: LigneTableauPrincipal | null = null;

  agInit(params: ICellRendererParams) {
    this.params = params;
    this.data = params.data;
  }

  refresh(params: ICellRendererParams) {
    this.params = params;
    return true;
  }

}
