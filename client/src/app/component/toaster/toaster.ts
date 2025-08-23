import {Component} from '@angular/core';
import {ToasterService} from '../../service/toaster.service';
import {ToasterMessage} from '../../entity/toaster-message';

@Component({
  selector: 'app-toaster',
  imports: [],
  templateUrl: './toaster.html',
  styleUrl: './toaster.scss',
  standalone: true,
})
export class Toaster {

  constructor(public toastService: ToasterService) {
  }

  remove($event: MouseEvent, toast: ToasterMessage) {
    this.toastService.remove(toast);
  }
}
