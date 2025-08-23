import {Injectable} from '@angular/core';
import {ToasterMessage} from '../entity/toaster-message';

@Injectable({
  providedIn: 'root'
})
export class ToasterService {

  private id = 1;

  toasts: ToasterMessage[] = [];

  show(message: string, classname = 'bg-primary text-white', delay = 5000) {
    let toast: ToasterMessage = {message, classname, id: this.id++, delay};
    this.toasts.push(toast);
    // Fermeture auto après "delay" ms
    toast.timeoutId = setTimeout(() => this.remove(toast), delay);
  }

  remove(toast: ToasterMessage) {
    this.toasts = this.toasts.filter(t => t.id !== toast.id);

    if (toast.timeoutId) {
      clearTimeout(toast.timeoutId);
    }
  }

}
