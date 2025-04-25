import {Injectable, NgZone} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environment/environment';
import {Node, Route} from './model';
import {Observable} from 'rxjs';


@Injectable({
    providedIn: 'root'
})
export class MATSimService {

    constructor(private http: HttpClient, private zone: NgZone) {
    }

    uploadNetwrok(file: FormData) {
        return this.http.post(`${environment.rootUrl}/network`, file)
    }

    /** retrieve MATSim network for the bounded box on the map */
    boundNetwork(south: number, west: number, north: number, east: number): Observable<Array<Node>> {
        return this.http.get<Array<Node>>(`${environment.rootUrl}/osm/nodes`,
            {params: {south: south.toString(), west: west.toString(), north: north.toString(), east: east.toString()}})
    }

    buildPlan(from: Node, to: Node): Observable<Route> {
        return this.http.post<Route>(`${environment.rootUrl}/plan`, {from: from, to: to})
    }

    randomPlan(num: number): Observable<Route> {
        return this.http.get<Route>(`${environment.rootUrl}/plan/random`, {params: {number: num}})
    }

    sse(num: number):Observable<Route> {
        return new Observable<Route>(observer => {
            const eventSource = new EventSource(`${environment.rootUrl}/plan/random-sse?number=${num}`)
            eventSource.onmessage = event => this.zone.run(() => observer.next(event.data))
            eventSource.addEventListener('periodic-event',
                    event => this.zone.run(() => observer.next(JSON.parse(event.data) as Route)))
            eventSource.onerror = err => {
                this.zone.run(() => observer.error(err))
                eventSource.close()
            }
            return () => eventSource.close()
        })
    }

    download() {
        return this.http.get(`${environment.rootUrl}/plan`, {responseType: 'blob'});
    }

}
