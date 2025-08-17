import {ICellRendererAngularComp} from 'ag-grid-angular';
import {ICellRendererParams} from 'ag-grid-community';
import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';
import {LigneTableauPrincipal} from './ligne-tableau-principal';
import {DatePipe} from '@angular/common';

@Component({
  selector: 'colour-cell',
  imports: [
    RouterLink,
    DatePipe
  ],
  // template: `@if (params.data.groupeId && params.value) {
  //   <a [routerLink]="['details',params.data.groupeId, params.value]">{{ params.value }}</a>
  // } @else {
  //   {{ params.value }}
  // }`
  //<a [style.colour]="params.color">{{params.value}}</a>`
  templateUrl: './git-cell-aggrid.html',
  styleUrl: './git-cell-aggrid.scss'
})
export class GitCellAgGrid implements ICellRendererAngularComp{

  params!: ICellRendererParams;
  // param2!:any;
  data:LigneTableauPrincipal|null=null;

  agInit(params: ICellRendererParams) {
    this.params = params;
    this.data=params.data;
    // this.param2=params.data;
    // console.log('params : ',this.params);
    // console.log('param2 : ',this.param2);
    // console.log('groupeId : ',this.params.data.groupId);
    // console.log('groupeId2 : ',(this.params as any).groupId);
  }

  refresh(params: ICellRendererParams) {
    this.params = params;
    // As we have updated the params we return true to let AG Grid know we have handled the refresh.
    // So AG Grid will not recreate the cell renderer from scratch.
    // console.log('groupeId bis : ',this.params.data.groupId);
    // console.log('groupeId2 bis : ',(this.params as any).groupId);
    return true;
  }

}
