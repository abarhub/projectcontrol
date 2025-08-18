import {ICellRendererAngularComp} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {Component} from '@angular/core';
import {LigneTableauPrincipal} from '../ligne-tableau-principal';
import {DatePipe} from '@angular/common';

@Component({
  selector: 'colour-cell',
  imports: [
    DatePipe
  ],
  templateUrl: './git-cell-aggrid.html',
  styleUrl: './git-cell-aggrid.scss'
})
export class GitCellAgGrid implements ICellRendererAngularComp {

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
