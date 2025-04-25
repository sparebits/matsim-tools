import {
    AfterViewInit,
    Component,
    EventEmitter,
    NgZone,
    OnDestroy,
    OnInit,
    ViewChild,
    WritableSignal
} from '@angular/core';
import {MatIcon} from "@angular/material/icon";
import {MatFabButton, MatIconButton} from "@angular/material/button";
import {MatToolbar} from "@angular/material/toolbar";
import {MatMenu, MatMenuItem, MatMenuTrigger} from "@angular/material/menu";
import {LeafletDirective, LeafletModule} from '@asymmetrik/ngx-leaflet';
import {
    circleMarker,
    CRS,
    Icon,
    icon,
    latLng,
    LatLng,
    latLngBounds,
    LatLngBounds,
    Layer,
    Map as LeafletMap,
    MapOptions,
    marker,
    Marker,
    point,
    polyline,
    tileLayer,
    tooltip
} from 'leaflet';
import {MatButtonToggle, MatButtonToggleGroup} from '@angular/material/button-toggle';
import {MatFormField, MatPrefix} from '@angular/material/form-field';
import {NgIf} from '@angular/common';
import {MatTooltip} from '@angular/material/tooltip';
import {MatCheckbox, MatCheckboxChange} from '@angular/material/checkbox';
import {FormsModule} from '@angular/forms';
import {MATSimService} from "./matsim.service";
import {CustomEventData, Link, MATSimEvent, Node, Route} from './model';
import {MatInput} from '@angular/material/input';
import {MatDialog} from '@angular/material/dialog';
import {ConfirmationDialogComponent} from './confirmation-dialog/confirmation-dialog.component';
import {environment} from '../environment/environment';
import {RoutesDialogComponent} from './routes-dialog/routes-dialog.component';


@Component({
    selector: 'app-root',
    imports: [
        MatIcon, MatIconButton, MatToolbar, MatMenu, MatMenuTrigger, MatMenuItem, LeafletModule,
        MatButtonToggleGroup, MatButtonToggle, MatPrefix, NgIf, NgIf, MatFabButton, MatTooltip, MatCheckbox, FormsModule,
        MatFormField, MatInput
    ],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, AfterViewInit, OnDestroy {

    title = 'matsim-network';

    @ViewChild(LeafletDirective, {static: false}) leafletDirective!: LeafletDirective
    map!: LeafletMap

    tileLayer = tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {maxZoom: 18, attribution: '...'})
    options: MapOptions = {
        crs: CRS.EPSG3857,
        layers: [this.tileLayer],
    }
    center: LatLng = latLng(43.8423155, 25.9752631);
    zoom: number | WritableSignal<number> = 15;
    caseLayers: Layer[] = []
    bounds: LatLngBounds = latLngBounds([])

    networkFile: File | null = null;
    planFile: File | null = null;
    eventsFile: File | null = null
    parser: DOMParser = new DOMParser()
    docNetwork?: Document

    nodes: Map<number, Node> = new Map<number, Node>()
    links: Map<string, Link> = new Map<string, Link>()
    events: Map<number, Array<MATSimEvent>> = new Map<number, Array<MATSimEvent>>()
    checked: any;

    planStart?: Node
    planEnd?: Node

    customEvent = new EventEmitter<CustomEventData>()
    departures = new EventEmitter<Array<MATSimEvent>>()
    arrivals = new EventEmitter<Array<MATSimEvent>>()
    speed: number = 1

    iconCar: Icon = icon({
        iconUrl: '/car.png',
        iconSize: [8, 8]
    })
    vehicles: Map<string, Marker> = new Map<string, Marker>()

    constructor(
        private matsimService: MATSimService,
        private dialog: MatDialog,
        private zone: NgZone
    ) {
    }

    ngOnInit(): void {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(pos => this.center = latLng(pos.coords.latitude, pos.coords.longitude))
        }
        this.customEvent.subscribe((data) => {
            console.log('Received in same component:', data.id);
        })
        this.departures.subscribe(this.process.bind(this))
        this.arrivals.subscribe(arrivals => console.debug('processing arrivals...'))
    }

    ngAfterViewInit(): void {
        this.map = this.leafletDirective.getMap()
    }

    onNetworkSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files?.length) {
            this.networkFile = input.files[0];
        }
        const formData = new FormData()
        // @ts-ignore
        formData.append('network', this.networkFile)
        this.matsimService.uploadNetwrok(formData).subscribe(resp => console.log(resp))
        const reader = new FileReader();
        console.debug("reding from network...")
        reader.onload = () => {
            this.docNetwork = this.parser.parseFromString(reader.result as string, 'application/xml');
            this.parseNetwork(this.docNetwork);
            this.showNodes()
        };
        // @ts-ignore
        reader.readAsText(this.networkFile);
    }

    private parseNetwork(doc: Document) {
        this.parseNodes(doc)
        this.parseLinks(doc)
        // this.showNodes()
    }

    private parseNodes(doc: Document) {
        const nodes = doc.getElementsByTagName('node');
        this.nodes = new Map<number, Node>()
        let minX = Number.MAX_VALUE, minY = Number.MAX_VALUE
        let maxX = Number.MIN_VALUE, maxY = Number.MIN_VALUE
        for (let i = 0; i < nodes.length; i++) {
            let node = <Node>{
                // @ts-ignore
                id: nodes[i].attributes['id'].nodeValue,
                // @ts-ignore
                x: nodes[i].attributes['x'].nodeValue,
                // @ts-ignore
                y: nodes[i].attributes['y'].nodeValue,
            }
            this.nodes.set(node.id, node)
            if (minX > node.x) minX = node.x
            if (minY > node.y) minY = node.y
            if (maxX < node.x) maxX = node.x
            if (maxY < node.y) maxY = node.y
        }
        this.bounds = latLngBounds([
            CRS.EPSG3857.unproject(point(minX, minY)),
            CRS.EPSG3857.unproject(point(maxX, maxY))
        ])
    }

    protected showNodes() {
        this.caseLayers = []
        this.nodes.forEach(node => {
            let n = CRS.EPSG3857.unproject(point(node.x, node.y))
            this.caseLayers
                .push(circleMarker(n, {radius: 3, color: "blue"})
                    .on('click', event => {
                        if (this.planStart) {
                            this.planEnd = node
                            this.matsimService.buildPlan(this.planStart, this.planEnd).subscribe(route => {
                                this.showRoute(route)
                            })
                            this.planStart = undefined
                        } else {
                            this.planStart = node
                            console.log(`waiting to select end node...`)
                        }
                    }))
        })
    }

    private parseLinks(doc: Document) {
        const links = doc.getElementsByTagName('link');
        this.links = new Map<string, Link>()
        for (let i = 0; i < links.length; i++) {
            let link = <Link>{
                // @ts-ignore
                id: links[i].attributes['id'].nodeValue,
                // @ts-ignore
                from: this.nodes.get(links[i].attributes['from'].nodeValue),
                // @ts-ignore
                to: this.nodes.get(links[i].attributes['to'].nodeValue),
                // @ts-ignore
                capacity: links[i].attributes['capacity'].nodeValue,
                // @ts-ignore
                speed: links[i].attributes['freespeed'].nodeValue,
            }
            this.links.set(String(link.id), link)
        }
    }

    onPlanSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files?.length) {
            this.planFile = input.files[0];
        }
        const reader = new FileReader();
        console.debug("reding from plan...")
        reader.onload = () => {
            let doc = this.parser.parseFromString(reader.result as string, 'application/xml');
            this.parsePlan(doc);
        };
        // @ts-ignore
        reader.readAsText(this.planFile);
    }

    onEventsSelected($event: Event) {
        const input = $event.target as HTMLInputElement;
        if (input.files?.length) {
            this.eventsFile = input.files[0];
        }
        const reader = new FileReader();
        console.debug("reding from events...")
        reader.onload = () => {
            this.parseEvents(this.parser.parseFromString(reader.result as string, 'application/xml'));
        };
        // @ts-ignore
        reader.readAsText(this.eventsFile);
    }

    private parsePlan(doc: Document) {
        const routes = doc.getElementsByTagName('route');
        for (let i = 0; i < routes.length; i++) {
            let linkIds = routes[i].innerHTML.split(' ')
            let nodes: Array<Node> = []
            // @ts-ignore
            nodes.push(this.links.get(linkIds[0])?.from)
            for (let j = 0; j < linkIds.length; j++) {
                nodes.push(<Node>this.links.get(linkIds[j])?.to)
            }
            this.caseLayers.push(polyline(nodes.map(n => CRS.EPSG3857.unproject(point(n.x, n.y))), {
                color: 'red',
                lineJoin: "round"
            }))
        }
    }

    private showRoute(route: Route) {
        let nodes: Array<Node> = []
        nodes.push(route.links[0]?.from)
        for (let j = 0; j < route.links.length; j++) {
            nodes.push(route.links[j]?.to)
        }
        // WARNING! this is the way route can appear immediately on the map!
        polyline(nodes.map(n => CRS.EPSG3857.unproject(point(n.x, n.y))), {
            color: 'red',
            lineJoin: "round"
        }).addTo(this.map)
    }

    showCapacity() {
        this.caseLayers = []
        this.links.forEach(l => {
            let options = this.weight(l.capacity, l.speed)
            let tt = tooltip({content: `id: ${l.id}`})
            this.caseLayers.push(
                polyline([
                    CRS.EPSG3857.unproject(point(l.from.x, l.from.y)),
                    CRS.EPSG3857.unproject(point(l.to.x, l.to.y)),
                ], {color: options[0], lineJoin: "round", weight: options[1]}).bindTooltip(tt)
            )
        })
    }

    weight(capacity: number, speed: number): [string, number] {
        if (capacity < 500) {
            return ["lightgreen", 1]
        } else if (capacity >= 500 && capacity < 1000) {
            return ["green", 2]
        } else if (capacity >= 1000 && capacity < 2000) {
            return ["yellow", 3]
        } else if (capacity >= 2000) {
            return ["red", 4]
        }
        return ["white", 0]
    }

    showMap($event: MatCheckboxChange) {
        if ($event.checked) {
            this.tileLayer.setOpacity(0)
        } else {
            this.tileLayer.setOpacity(100)
        }
    }

    downloadPlans() {
        this.matsimService.download().subscribe(blob => {
            const a = document.createElement('a');
            const objectUrl = URL.createObjectURL(blob);
            a.href = objectUrl;
            a.download = 'plans.xml'; // Set the filename
            a.click();
            URL.revokeObjectURL(objectUrl);
        })
    }

    totalSeconds: number | undefined = 10
    counter: number | undefined = 0

    emulate() {
        const sortedKeys = [...this.events.keys()].sort((a, b) => a - b)
        this.counter = sortedKeys.at(0)
        this.totalSeconds = sortedKeys.at(-1)
        let key = sortedKeys.shift()
        this.intervalId = setInterval(() => {
            if (this.counter == key) {
                let events = this.events.get(key!)
                this.departures.emit(events)
                key = sortedKeys.shift()
            }
            // @ts-ignore
            this.counter++;
            // @ts-ignore
            if (this.counter >= this.totalSeconds) {
                clearInterval(this.intervalId);
                console.debug('emulation complete!')
            }
        }, 1000 / this.speed)
    }

    intervalId: any

    ngOnDestroy(): void {
        if (this.intervalId) {
            clearInterval(this.intervalId);
        }
    }

    /** Create structure of events loaded from MATSim xml events file */
    private parseEvents(doc: Document) {
        const routes = doc.getElementsByTagName('event');
        this.events = new Map<number, Array<MATSimEvent>>()
        for (let i = 0; i < routes.length; i++) {
            let time = Number(routes[i].attributes.getNamedItem('time')?.nodeValue)
            let evtList = this.events.get(time)
            if (!evtList) {
                evtList = new Array<MATSimEvent>()
                this.events.set(time, evtList)
            }
            // @ts-ignore
            evtList.push(<MATSimEvent>{
                time: time,
                type: routes[i].attributes.getNamedItem('type')?.nodeValue,
                person: routes[i].attributes.getNamedItem('person')?.nodeValue,
                vehicle: routes[i].attributes.getNamedItem('vehicle')?.nodeValue,
                link: routes[i].attributes.getNamedItem('link')?.nodeValue,
            })
        }
    }

    private process(events: MATSimEvent[]) {
        events.forEach(e => {
            if ('vehicle enters traffic' === e.type) {
                console.debug(`vehicle: ${e.vehicle} enters traffic`)
                let n = this.links.get(e.link)?.from
                const m = marker(CRS.EPSG3857.unproject(point(<number>n?.x, <number>n?.y)), {icon: this.iconCar})
                m.addTo(this.map)
                this.vehicles.set(e.vehicle, m)
            } else if ('vehicle leaves traffic' === e.type) {
                console.debug(`vehicle: ${e.vehicle} leaves traffic`)
                const m = this.vehicles.get(e.vehicle)
                this.map.removeLayer(<Layer>m)
                this.vehicles.delete(e.vehicle)
            } else if ('entered link' === e.type) {
                const m = this.vehicles.get(e.vehicle)
                const n = this.links.get(e.link)?.from
                m?.setLatLng(CRS.EPSG3857.unproject(point(<number>n?.x, <number>n?.y)))
            }
        })
    }

    /** Use bounded map rectangle to create MATSim network */
    boundMatsim() {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '300px',
            data: {
                type: 'confirmation',
                title: 'Confirm Operation',
                message: 'This operation will destroy the old scenario! Are you sure you want to proceed?'
            }
        })
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                if (this.map) {
                    const bounds = this.map.getBounds(); // Get the bounding rectangle
                    const southWest = bounds.getSouthWest(); // Southwest corner
                    const northEast = bounds.getNorthEast(); // Northeast corner
                    this.matsimService.boundNetwork(southWest.lat, southWest.lng, northEast.lat, northEast.lng)
                        .subscribe(r => {
                            this.nodes = new Map<number, Node>()
                            r.forEach(node => this.nodes.set(node.id, node))
                            this.showNodes()
                        })
                } else {
                    console.error('Map is not initialized.');
                }
            } else {
                console.log('User canceled the operation.');
            }
        })
    }

    randomPlan() {
        this.matsimService.randomPlan(50).subscribe(route => this.showRoute(route))
    }

    randomRoutes() {
        const dialogRef = this.dialog.open(RoutesDialogComponent, {
            width: '400px',
            data: {population: 10}
        })
        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.matsimService.sse(result).subscribe({
                    next: data => this.showRoute(data),
                    error: err => {
                        if (err.data) {
                            console.error('SSE error: ' + JSON.parse(err.data).details)
                            this.dialog.open(ConfirmationDialogComponent, {
                                width: '300px',
                                data: {
                                    type: 'error',
                                    title: 'Error',
                                    message: JSON.parse(err.data).details
                                }
                            })
                        }
                    }
                })
            }
        })
    }
}
