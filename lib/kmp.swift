//
//  koin.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 24/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Shared

class SwiftKClass<T>: NSObject, KotlinKClass {
  func isInstance(value: Any?) -> Bool { value is T }
  var qualifiedName: String? { String(reflecting: T.self) }
  var simpleName: String? { String(describing: T.self) }
}

func KClass<T>(for type: T.Type) -> KotlinKClass {
  SwiftType(type: type, swiftClazz: SwiftKClass<T>()).getClazz()
}

func inject<T>(
  qualifier: Koin_coreQualifier? = nil,
  parameters: (() -> Koin_coreParametersHolder)? = nil
) -> T {
    KoinUtils.shared.koinGet(kClass: KClass(for: T.self), qualifier: qualifier, parameter: parameters) as! T
}

