import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {Toaster} from './component/toaster/toaster';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toaster],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('projectcontrolclient');
}
