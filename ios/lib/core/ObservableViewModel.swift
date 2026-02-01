//
//  ObservableViewModel.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

extension ObservableViewModel{
    func binding(
        getValue: @escaping (State) -> String,
        getIntent: @escaping (String) -> Intent
    ) -> Binding<String> {
        Binding { [self] in
            getValue(state)
        } set: { newValue in
            self.handle(intent: getIntent(newValue))
        }
    }
    
    func binding(
        getValue: @escaping (State) -> String?,
        getIntent: @escaping (String?) -> Intent
    ) -> Binding<String?> {
        Binding<String?>(
            get: { [self] in getValue(state) },
            set: { newValue in self.handle(intent: getIntent(newValue)) }
        )
    }
}

@Observable
class ObservableViewModel<State: BaseState, Intent: BaseIntent, Event: BaseEvent, VM: BaseViewModel<State, Intent, Event>>{
        
    let vm: VM
    
    var state: State
    var event: Event?
    
    
    init(){
        self.vm = inject()
        self.state = vm.state.value
    }
    
    func handle(intent: Intent){
        vm.handle(intent: intent)
    }
    
    func collect() async {
        async let eventTask: Void = collectEvents()
        async let stateTask: Void = collectState()
        
        _ = await (eventTask, stateTask)
    }
    
    private func collectState() async {
        for await newState in vm.state {
            state = newState
        }
    }
    
    private func collectEvents() async {
        for await newEvent in vm.eventsFlow {
            event = newEvent
        }
    }
    
}

