import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {TableauPrincipal} from './tableau-principal/tableau-principal';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TableauPrincipal],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('projectcontrolclient');
}
