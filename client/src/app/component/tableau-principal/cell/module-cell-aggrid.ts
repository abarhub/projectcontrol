import {ICellRendererAngularComp} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {Component} from '@angular/core';
import {LigneTableauPrincipal} from './../ligne-tableau-principal';

@Component({
  selector: 'colour-cell',
  imports: [],
  templateUrl: './module-cell-aggrid.html',
})
export class ModuleCellAgGrid implements ICellRendererAngularComp {

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
