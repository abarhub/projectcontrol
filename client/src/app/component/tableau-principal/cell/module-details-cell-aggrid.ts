import {ICellRendererAngularComp} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {Component} from '@angular/core';
import {LigneTableauPrincipal} from './../ligne-tableau-principal';
import {KeyValuePipe} from '@angular/common';

@Component({
  selector: 'colour-cell',
  imports: [
    KeyValuePipe
  ],
  templateUrl: './module-details-cell-aggrid.html',
})
export class ModuleDetailsCellAgGrid implements ICellRendererAngularComp {

  params!: ICellRendererParams;
  // param2!:any;
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
