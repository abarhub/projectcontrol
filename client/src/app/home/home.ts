import { Component } from '@angular/core';
import {TableauPrincipal} from '../tableau-principal/tableau-principal';
import {MenuPrincipal} from '../menu-principal/menu-principal';

@Component({
  selector: 'app-home',
  imports: [
    TableauPrincipal,
    MenuPrincipal
  ],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home {

}
